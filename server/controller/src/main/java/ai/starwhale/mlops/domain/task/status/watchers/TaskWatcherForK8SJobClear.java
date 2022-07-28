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

package ai.starwhale.mlops.domain.task.status.watchers;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(6)
@Slf4j
@Component
public class TaskWatcherForK8SJobClear implements TaskStatusChangeWatcher {

    final K8sClient k8sClient;

    public TaskWatcherForK8SJobClear(K8sClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    @Override
    public void onTaskStatusChange(Task task,
        TaskStatus oldStatus) {
        if(task.getStatus() != TaskStatus.SUCCESS && task.getStatus() != TaskStatus.FAIL){
            return;
        }

        try {
            k8sClient.deleteJob(task.getId().toString());
        } catch (ApiException e) {
            log.error("delete k8s job failed",e);
        }

    }
}
