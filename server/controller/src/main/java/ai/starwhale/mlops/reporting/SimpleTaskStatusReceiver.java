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

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * the processor for every report from Agent
 */
@Slf4j
@Service
public class SimpleTaskStatusReceiver implements TaskStatusReceiver {

    final HotJobHolder jobHolder;

    final TaskMapper taskMapper;

    public SimpleTaskStatusReceiver(HotJobHolder jobHolder, TaskMapper taskMapper) {
        this.jobHolder = jobHolder;
        this.taskMapper = taskMapper;
    }

    @Override
    public void receive(List<ReportedTask> reportedTasks) {

        reportedTasks.forEach(reportedTask -> {
            Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(reportedTask.getId()));

            if (null == optionalTasks || optionalTasks.isEmpty()) {
                log.warn("un-cached tasks reported {}, status directly update to DB", reportedTask.getId());
                if (reportedTask.getRetryCount() != null && reportedTask.getRetryCount() > 0) {
                    taskMapper.updateRetryNum(reportedTask.getId(), reportedTask.getRetryCount());
                }
                if (reportedTask.status != TaskStatus.UNKNOWN) {
                    taskMapper.updateTaskStatus(List.of(reportedTask.getId()), reportedTask.getStatus());
                }
                return;
            }
            if (reportedTask.getRetryCount() != null && reportedTask.getRetryCount() > 0) {
                optionalTasks.forEach(task -> task.setRetryNum(reportedTask.getRetryCount()));
            }
            if (reportedTask.status != TaskStatus.UNKNOWN) {
                optionalTasks.forEach(task -> task.updateStatus(reportedTask.getStatus()));
            }
        });

    }
}
