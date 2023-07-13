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

import ai.starwhale.mlops.domain.dataset.build.log.BuildLogCollector;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.reporting.TaskModifyReceiver;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class PodEventHandler implements ResourceEventHandler<V1Pod> {

    final TaskLogK8sCollector taskLogK8sCollector;
    final BuildLogCollector buildLogCollector;
    final TaskModifyReceiver taskModifyReceiver;
    final HotJobHolder jobHolder;

    public PodEventHandler(
            TaskLogK8sCollector taskLogK8sCollector,
            BuildLogCollector buildLogCollector,
            TaskModifyReceiver taskModifyReceiver,
            HotJobHolder jobHolder) {
        this.taskLogK8sCollector = taskLogK8sCollector;
        this.buildLogCollector = buildLogCollector;
        this.taskModifyReceiver = taskModifyReceiver;
        this.jobHolder = jobHolder;
    }

    @Override
    public void onAdd(V1Pod obj) {
    }

    @Override
    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
        var metaData = newObj.getMetadata();
        if (metaData == null) {
            return;
        }
        var labels = metaData.getLabels();
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        var type = labels.get(K8sJobTemplate.JOB_TYPE_LABEL);
        if (StringUtils.hasText(type)) {
            log.debug("job({}) {} for {}.", type, "onUpdate", getJobNameAsId(newObj));
            switch (type) {
                case K8sJobTemplate.WORKLOAD_TYPE_EVAL:
                    updateEvalTask(newObj);
                    collectLog(newObj, type);
                    break;
                case K8sJobTemplate.WORKLOAD_TYPE_DATASET_BUILD:
                    // build dataset only receive fail or success status in jobEventHandler
                    collectLog(newObj, type);
                    break;
                default:
            }
        }

    }

    @Override
    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) {
    }

    private Long getJobNameAsId(V1Pod pod) {
        String jobName = pod.getMetadata().getLabels().get("job-name");
        if (null == jobName || jobName.isBlank()) {
            log.info("no id found for pod {}", jobName);
            return null;
        }
        Long id;
        try {
            id = Long.valueOf(jobName);
        } catch (Exception e) {
            log.warn("id is not number {}", jobName);
            id = null;
        }
        return id;
    }

    private void updateEvalTask(V1Pod pod) {
        if (null == pod.getStatus() || null == pod.getStatus().getPhase()) {
            return;
        }

        // https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination
        if (pod.getMetadata() != null && pod.getMetadata().getDeletionTimestamp() != null) {
            log.info("pod {} is being deleted", pod.getMetadata().getName());
            return;
        }
        Long tid = getJobNameAsId(pod);
        if (tid == null) {
            return;
        }

        TaskStatus taskStatus;
        var phase = pod.getStatus().getPhase();
        if (StringUtils.hasText(phase)) {
            switch (phase) {
                /*
                Pending The Pod has been accepted by the Kubernetes cluster,
                but one or more of the containers has not been set up and made ready to run.
                This includes time a Pod spends waiting to be scheduled
                as well as the time spent downloading container images over the network.
                 */
                case "Pending":
                    taskStatus = TaskStatus.PREPARING;
                    break;
                /*
                Running The Pod has been bound to a node, and all the containers have been created.
                At least one container is still running, or is in the process of starting or restarting.
                 */
                case "Running":
                    taskStatus = TaskStatus.RUNNING;
                    break;
                default:
                    return;

            }
        } else {
            return;
        }

        Long startTime = null;
        if (pod.getStatus() != null) {
            startTime = Util.k8sTimeToMs(pod.getStatus().getStartTime());
        }
        log.debug("task:{} status changed to {}.", tid, taskStatus);
        var report = ReportedTask.builder()
                .id(tid)
                .status(taskStatus)
                .ip(pod.getStatus().getPodIP())
                .startTimeMillis(startTime)
                .stopTimeMillis(null)
                .build();
        taskModifyReceiver.receive(List.of(report));
    }

    private void collectLog(V1Pod pod, String type) {
        log.debug("collect log for pod {} status {}", pod.getMetadata().getName(), pod.getStatus());
        if (null == pod.getStatus()
                || null == pod.getStatus().getContainerStatuses()
                || null == pod.getStatus().getContainerStatuses().get(0)
                || null == pod.getStatus().getContainerStatuses().get(0).getState()
                || null == pod.getStatus().getContainerStatuses().get(0).getState().getTerminated()
        ) {
            return;
        }
        if (pod.getMetadata() != null && pod.getMetadata().getDeletionTimestamp() != null) {
            return;
        }
        Long id;
        switch (type) {
            case K8sJobTemplate.WORKLOAD_TYPE_EVAL:
                id = getJobNameAsId(pod);
                if (id != null) {
                    Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(id));
                    if (CollectionUtils.isEmpty(optionalTasks)) {
                        log.warn("no tasks found for pod {}", pod.getMetadata().getName());
                        return;
                    }
                    Task task = optionalTasks.stream().findAny().get();
                    taskLogK8sCollector.collect(task);
                }
                break;
            case K8sJobTemplate.WORKLOAD_TYPE_DATASET_BUILD:
                id = Long.parseLong(pod.getMetadata().getAnnotations().get("id"));
                buildLogCollector.collect(id);
                break;
            default:
        }


    }

}
