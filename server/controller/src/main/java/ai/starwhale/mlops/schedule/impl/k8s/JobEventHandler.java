/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.schedule.impl.k8s;

import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JobEventHandler implements ResourceEventHandler<V1Job> {

    private final TaskReportReceiver taskReportReceiver;
    private final TaskStatusMachine taskStatusMachine;
    private final RuntimeService runtimeService;
    private final K8sClient k8sClient;

    public JobEventHandler(
            TaskReportReceiver taskReportReceiver,
            TaskStatusMachine taskStatusMachine,
            RuntimeService runtimeService,
            K8sClient k8sClient) {
        this.taskReportReceiver = taskReportReceiver;
        this.taskStatusMachine = taskStatusMachine;
        this.runtimeService = runtimeService;
        this.k8sClient = k8sClient;
    }

    @Override
    public void onAdd(V1Job obj) {
        dispatch(obj, "onAdd");
    }

    @Override
    public void onUpdate(V1Job oldObj, V1Job newObj) {
        dispatch(newObj, "onUpdate");
    }

    @Override
    public void onDelete(V1Job obj, boolean deletedFinalStateUnknown) {
        log.debug("job deleted for {} {}", jobName(obj), obj.getStatus());
        updateEvalTask(obj, true);
    }

    private String jobName(V1Job obj) {
        return obj.getMetadata().getName();
    }

    private void dispatch(V1Job job, String event) {
        var metaData = job.getMetadata();
        if (metaData == null) {
            return;
        }
        var labels = metaData.getLabels();
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        var type = labels.get(K8sJobTemplate.JOB_TYPE_LABEL);
        if (StringUtils.hasText(type)) {
            log.debug("job({}) {} for {} with status {}", type, event, jobName(job), job.getStatus());
            switch (type) {
                case K8sJobTemplate.WORKLOAD_TYPE_EVAL:
                    updateEvalTask(job, false);
                    break;
                case K8sJobTemplate.WORKLOAD_TYPE_IMAGE_BUILDER:
                    updateImageBuildTask(job);
                    break;
                default:
            }
        }
    }

    private void updateImageBuildTask(V1Job job) {
        V1JobStatus status = job.getStatus();
        if (status == null) {
            return;
        }
        var version = jobName(job);
        var image = job.getMetadata().getAnnotations().get("image");
        if (!StringUtils.hasText(version) || !StringUtils.hasText(image)) {
            return;
        }
        if (null != status.getSucceeded()) {
            log.info("image:{} build success", image);
            runtimeService.updateBuiltImage(version, image);
        }
    }

    private void updateEvalTask(V1Job job, boolean onDelete) {
        V1JobStatus status = job.getStatus();
        if (status == null) {
            return;
        }
        var jobName = jobName(job);
        Long stopTime = null;
        String jobFailedReason = null;
        TaskStatus taskStatus = TaskStatus.UNKNOWN;
        // https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#jobstatus-v1-batch
        //  The latest available observations of an object's current state.
        //  When a Job fails, one of the conditions will have type "Failed" and status true.
        //  When a Job is suspended, one of the conditions will have type "Suspended" and status true;
        //  when the Job is resumed, the status of this condition will become false.
        //  When a Job is completed, one of the conditions will have type "Complete" and status true.
        //  More info: https://kubernetes.io/docs/concepts/workloads/controllers/jobs-run-to-completion/
        List<V1JobCondition> conditions = status.getConditions();
        if (null != conditions) {
            List<V1JobCondition> collect = conditions.stream().filter(c -> "True".equalsIgnoreCase(c.getStatus()))
                    .collect(Collectors.toList());
            if (collect.size() == 0) {
                log.warn("no True status conditions for job {}", conditionsLogString(conditions));
            } else {
                if (collect.size() > 1) {
                    log.warn("multiple True status conditions for job received {} random one is chosen",
                            conditionsLogString(conditions));
                }
                var condition = collect.get(0);
                String type = condition.getType();
                if ("Failed".equalsIgnoreCase(type)) {
                    taskStatus = TaskStatus.FAIL;
                    stopTime = Util.k8sTimeToMs(condition.getLastTransitionTime());
                    log.debug("job status changed for {} is failed {}", jobName, status);
                    jobFailedReason = joinReasons(condition.getReason(), condition.getMessage());
                } else if ("Complete".equalsIgnoreCase(type)) {
                    taskStatus = TaskStatus.SUCCESS;
                    stopTime = Util.k8sTimeToMs(condition.getLastTransitionTime());
                } else if ("Suspended".equalsIgnoreCase(type)) {
                    log.warn("unexpected task status detected {}", type);
                } else {
                    log.warn("unknown task status detected {}", type);
                }
            }
        }

        // we assume that the job is cancelled if it is not failed when delete
        if (taskStatus != TaskStatus.SUCCESS && taskStatus != TaskStatus.FAIL && onDelete) {
            taskStatus = TaskStatus.CANCELED;
        }

        Long startTime = null;
        if (taskStatusMachine.isFinal(taskStatus)) {
            startTime = getPodStartTime(jobName);
            if (startTime == null) {
                log.warn("no pod start time found for job {}, use now", jobName);
                startTime = System.currentTimeMillis();
            }
            if (stopTime == null) {
                stopTime = System.currentTimeMillis();
                log.warn("no pod stop time found for job {}, use now", jobName);
            }
        }

        // prefer using the pod failed reason, it has more details
        var podFailedReason = getPodFailedReason(jobName);

        var failedReason = "";
        if (StringUtils.hasText(jobFailedReason)) {
            failedReason = "job failed: " + jobFailedReason;
        }
        if (StringUtils.hasText(podFailedReason)) {
            failedReason = failedReason + "\npod failed: " + podFailedReason;
        }

        // retry number here is not reliable, it only counts failed pods that is not deleted
        Integer retryNum = null != status.getFailed() ? status.getFailed() : 0;
        var report = ReportedTask.builder()
                .id(Long.parseLong(jobName))
                .status(taskStatus)
                .startTimeMillis(startTime)
                .stopTimeMillis(stopTime)
                .retryCount(retryNum)
                .failedReason(StringUtils.hasText(failedReason) ? failedReason : null)
                .build();
        taskReportReceiver.receive(List.of(report));
    }

    private String conditionsLogString(List<V1JobCondition> conditions) {
        return String.join(",",
                conditions.stream().map(c -> String.format("type %s status %s", c.getType(), c.getStatus())).collect(
                        Collectors.toSet()));
    }

    private String joinReasons(String... reasons) {
        var items = Arrays.stream(reasons).filter(Objects::nonNull).collect(Collectors.toList());
        if (items.isEmpty()) {
            return null;
        }
        return String.join(", ", items);
    }

    private Long getPodStartTime(String jobId) {
        List<V1Pod> pods = k8sClient.getPodsByJobNameQuietly(jobId);
        var startTimes = pods.stream().filter(p -> p.getStatus() != null)
                .map(p -> Util.k8sTimeToMs(p.getStatus().getStartTime()))
                .filter(Objects::nonNull).collect(Collectors.toList());

        if (startTimes.size() == 0) {
            return null;
        }
        return startTimes.stream().min(Long::compareTo).get();
    }

    private String getPodFailedReason(String jobId) {
        List<V1Pod> pods = k8sClient.getPodsByJobNameQuietly(jobId);

        var failedReasons = pods.stream().filter(p -> p.getStatus() != null)
                .filter(p -> Objects.equals(p.getStatus().getPhase(), "Failed"))
                .map(p -> joinReasons(p.getStatus().getReason(), p.getStatus().getMessage()))
                .filter(Objects::nonNull).collect(Collectors.toList());

        if (failedReasons.size() == 0) {
            return null;
        }
        // join all failed reasons
        return String.join("\n", failedReasons);
    }
}
