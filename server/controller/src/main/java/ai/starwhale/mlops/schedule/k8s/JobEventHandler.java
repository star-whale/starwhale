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

package ai.starwhale.mlops.schedule.k8s;

import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.reporting.TaskModifyReceiver;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobStatus;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JobEventHandler implements ResourceEventHandler<V1Job> {

    private final TaskModifyReceiver taskModifyReceiver;
    private final RuntimeService runtimeService;

    public JobEventHandler(TaskModifyReceiver taskModifyReceiver, RuntimeService runtimeService) {
        this.taskModifyReceiver = taskModifyReceiver;
        this.runtimeService = runtimeService;
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
                    updateEvalTask(job);
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

    private void updateEvalTask(V1Job job) {
        V1JobStatus status = job.getStatus();
        if (status == null) {
            return;
        }
        Long stopTime = null;
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
                    log.debug("job status changed for {} is failed {}", jobName(job), status);
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
        // warning: Represents time when the job controller started processing a job.
        // When a Job is created in the suspended state, this field is not set until the first time it is resumed.
        // This field is reset every time a Job is resumed from suspension.
        // It is represented in RFC3339 form and is in UTC.
        // https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.25/#jobstatus-v1-batch
        // get the start time of the job to fill in the task status, beware that the start time is not reliable
        // we only use it to fill in the task start time when the start time filed is not set
        Long startTime = Util.k8sTimeToMs(status.getStartTime());

        // retry number here is not reliable, it only counts failed pods that is not deleted
        Integer retryNum = null != status.getFailed() ? status.getFailed() : 0;
        var report = ReportedTask.builder()
                .id(Long.parseLong(jobName(job)))
                .status(taskStatus)
                .startTimeMillis(startTime)
                .stopTimeMillis(stopTime)
                .retryCount(retryNum)
                .build();
        taskModifyReceiver.receive(List.of(report));
    }

    private String conditionsLogString(List<V1JobCondition> conditions) {
        return String.join(",",
                conditions.stream().map(c -> String.format("type %s status %s", c.getType(), c.getStatus())).collect(
                        Collectors.toSet()));
    }
}
