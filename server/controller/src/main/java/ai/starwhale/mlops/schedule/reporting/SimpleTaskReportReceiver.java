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

package ai.starwhale.mlops.schedule.reporting;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * the processor for every report from Agent
 */
@Slf4j
@Service
public class SimpleTaskReportReceiver implements TaskReportReceiver {

    final HotJobHolder jobHolder;

    final TaskMapper taskMapper;

    public SimpleTaskReportReceiver(HotJobHolder jobHolder, TaskMapper taskMapper) {
        this.jobHolder = jobHolder;
        this.taskMapper = taskMapper;
    }

    @Override
    public void receive(List<ReportedTask> reportedTasks) {

        reportedTasks.forEach(reportedTask -> {
            if (reportedTask.getFailedReason() != null) {
                taskMapper.updateFailedReason(reportedTask.getId(), reportedTask.getFailedReason());
            }

            Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(reportedTask.getId()));

            if (null == optionalTasks || optionalTasks.isEmpty()) {
                log.warn("un-cached tasks reported {}, status directly update to DB", reportedTask.getId());
                if (reportedTask.getRetryCount() != null && reportedTask.getRetryCount() > 0) {
                    taskMapper.updateRetryNum(reportedTask.getId(), reportedTask.getRetryCount());
                }
                if (StringUtils.hasText(reportedTask.getIp())) {
                    taskMapper.updateIp(reportedTask.getId(), reportedTask.getIp());
                }
                if (reportedTask.status != TaskStatus.UNKNOWN) {
                    taskMapper.updateTaskStatus(List.of(reportedTask.getId()), reportedTask.getStatus());
                }
                // update start time
                if (reportedTask.getStartTimeMillis() != null) {
                    var tm = new Date(reportedTask.getStartTimeMillis());
                    taskMapper.updateTaskStartedTimeIfNotSet(reportedTask.getId(), tm);
                }
                if (reportedTask.getStopTimeMillis() != null) {
                    var tm = new Date(reportedTask.getStopTimeMillis());
                    taskMapper.updateTaskFinishedTimeIfNotSet(reportedTask.getId(), tm);
                }
                return;
            }
            if (reportedTask.getRetryCount() != null && reportedTask.getRetryCount() > 0) {
                optionalTasks.forEach(task -> task.setRetryNum(reportedTask.getRetryCount()));
                taskMapper.updateRetryNum(reportedTask.getId(), reportedTask.getRetryCount());
            }
            if (StringUtils.hasText(reportedTask.getIp())) {
                optionalTasks.forEach(task -> task.setIp(reportedTask.getIp()));
                taskMapper.updateIp(reportedTask.getId(), reportedTask.getIp());
            }
            if (reportedTask.status != TaskStatus.UNKNOWN) {
                optionalTasks.forEach(task -> {
                    // update time before status because only the updateStatus will trigger the watcher
                    // TODO optimize the update time logic
                    if (task.getStartTime() == null) {
                        task.setStartTime(reportedTask.getStartTimeMillis());
                    }
                    if (task.getFinishTime() == null) {
                        task.setFinishTime(reportedTask.getStopTimeMillis());
                    }
                    task.updateStatus(reportedTask.getStatus());
                });
            }
        });

    }
}
