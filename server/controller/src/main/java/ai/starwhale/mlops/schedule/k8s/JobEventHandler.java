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

import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.reporting.TaskStatusReceiver;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobStatus;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobEventHandler implements ResourceEventHandler<V1Job> {

    private final TaskStatusReceiver taskStatusReceiver;

    public JobEventHandler(TaskStatusReceiver taskStatusReceiver) {
        this.taskStatusReceiver = taskStatusReceiver;
    }

    @Override
    public void onAdd(V1Job obj) {
        log.info("job added for {} with status {}", jobName(obj),
                obj.getStatus());
        updateToSw(obj);
    }

    private String jobName(V1Job obj) {
        return obj.getMetadata().getName();
    }

    @Override
    public void onUpdate(V1Job oldObj, V1Job newObj) {
        updateToSw(newObj);
    }

    private void updateToSw(V1Job newObj) {
        V1JobStatus status = newObj.getStatus();
        if (status == null) {
            return;
        }
        TaskStatus taskStatus = TaskStatus.UNKNOWN;
        Integer retryNum = null;
        // one task one k8s job
        if (null != status.getSucceeded()) {
            taskStatus = TaskStatus.SUCCESS;
            log.info("job status changed for {} is success  {}", jobName(newObj), status);
        } else {
            if (null != status.getActive()) {
                // running(failed == null) or restarting(failed != null)
                // running contains two stages:pending and running, these state changes are judged by podEventHandler
                if (null != status.getFailed()) {
                    retryNum = status.getFailed();
                    log.debug("job {} is restarting, retry num {}", jobName(newObj), status.getFailed());
                }
            } else {
                if (null != status.getFailed()) {
                    taskStatus = TaskStatus.FAIL;
                    log.debug("job status changed for {} is failed {}", jobName(newObj), status);
                }
            }
        }
        taskStatusReceiver.receive(List.of(new ReportedTask(taskIdOf(newObj), taskStatus, retryNum)));
    }

    private long taskIdOf(V1Job newObj) {
        return Long.parseLong(jobName(newObj));
    }

    @Override
    public void onDelete(V1Job obj, boolean deletedFinalStateUnknown) {
        log.info("job deleted for {} {}", jobName(obj),
                obj.getStatus());
        // taskStatusReceiver.receive(List.of(new ReportedTask(Long.parseLong(jobName(obj)),TaskStatus.CANCELED)));
    }
}
