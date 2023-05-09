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

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.reporting.TaskStatusReceiver;
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
    final TaskStatusReceiver taskStatusReceiver;
    final HotJobHolder jobHolder;

    public PodEventHandler(
            TaskLogK8sCollector taskLogK8sCollector, TaskStatusReceiver taskStatusReceiver, HotJobHolder jobHolder) {
        this.taskLogK8sCollector = taskLogK8sCollector;
        this.taskStatusReceiver = taskStatusReceiver;
        this.jobHolder = jobHolder;
    }

    @Override
    public void onAdd(V1Pod obj) {
    }

    @Override
    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
        // one task one k8s job
        updateEvalTask(newObj);
        collectLog(newObj);
    }

    @Override
    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) {
    }

    private Long getTaskId(V1Pod pod) {
        String taskId = pod.getMetadata().getLabels().get("job-name");
        if (null == taskId || taskId.isBlank()) {
            log.info("no task id found for pod {}", taskId);
            return null;
        }
        Long tid;
        try {
            tid = Long.valueOf(taskId);
        } catch (Exception e) {
            log.warn("task id is not number {}", taskId);
            tid = null;
        }
        return tid;
    }

    private void updateEvalTask(V1Pod pod) {
        if (null == pod.getStatus() || null == pod.getStatus().getPhase()) {
            return;
        }
        Long tid = getTaskId(pod);
        if (tid != null) {
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
                    Running The Pod has been bound to a node, and all of the containers have been created.
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

            // make status as running when pod condition contains PodScheduled
            // https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-conditions
            if (pod.getStatus().getConditions() != null) {
                var podScheduled = pod.getStatus().getConditions().stream()
                        .filter(c -> StringUtils.hasText(c.getStatus())
                                && c.getStatus().equals("True")
                                && StringUtils.hasText(c.getType())
                                && c.getType().equals("PodScheduled"))
                        .findAny();
                if (podScheduled.isPresent()) {
                    taskStatus = TaskStatus.RUNNING;
                }
            }

            log.debug("task:{} status changed to {}.", tid, taskStatus);
            taskStatusReceiver.receive(List.of(new ReportedTask(tid, taskStatus, null)));
        }
    }

    private void collectLog(V1Pod pod) {
        if (null == pod.getStatus()
                || null == pod.getStatus().getContainerStatuses()
                || null == pod.getStatus().getContainerStatuses().get(0)
                || null == pod.getStatus().getContainerStatuses().get(0).getState()
                || null == pod.getStatus().getContainerStatuses().get(0).getState().getTerminated()
        ) {
            return;
        }
        Long tid = getTaskId(pod);
        if (tid != null) {
            Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(tid));
            if (CollectionUtils.isEmpty(optionalTasks)) {
                log.warn("no tasks found for pod {}", pod.getMetadata().getName());
                return;
            }
            Task task = optionalTasks.stream().findAny().get();
            taskLogK8sCollector.collect(task);
        }
    }

}
