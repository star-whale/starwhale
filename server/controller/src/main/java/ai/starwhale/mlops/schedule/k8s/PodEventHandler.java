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
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class PodEventHandler implements ResourceEventHandler<V1Pod> {

    final TaskLogK8sCollector taskLogK8sCollector;

    final HotJobHolder jobHolder;


    public PodEventHandler(TaskLogK8sCollector taskLogK8sCollector, HotJobHolder jobHolder) {
        this.taskLogK8sCollector = taskLogK8sCollector;
        this.jobHolder = jobHolder;
    }

    @Override
    public void onAdd(V1Pod obj) {
    }

    @Override
    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
        if (null == newObj.getStatus()
                || null == newObj.getStatus().getContainerStatuses()
                || null == newObj.getStatus().getContainerStatuses().get(0)
                || null == newObj.getStatus().getContainerStatuses().get(0).getState()
                || null == newObj.getStatus().getContainerStatuses().get(0).getState().getTerminated()
        ) {
            return;
        }
        String taskId = newObj.getMetadata().getLabels().get("job-name");
        if (null == taskId || taskId.isBlank()) {
            log.info("no task id found for pod {}", taskId);
            return;
        }
        Long tid;
        try {
            tid = Long.valueOf(taskId);
        } catch (Exception e) {
            log.warn("task id is not number {}", taskId);
            return;
        }
        Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(tid));
        if (CollectionUtils.isEmpty(optionalTasks)) {
            log.warn("no tasks found for pod {}", newObj.getMetadata().getName());
            return;
        }
        Task task = optionalTasks.stream().findAny().get();
        taskLogK8sCollector.collect(task);
    }

    @Override
    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) {
    }
}
