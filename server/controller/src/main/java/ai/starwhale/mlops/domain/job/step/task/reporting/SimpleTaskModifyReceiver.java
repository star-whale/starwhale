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

package ai.starwhale.mlops.domain.job.step.task.reporting;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
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
public class SimpleTaskModifyReceiver implements TaskModifyReceiver {

    final HotJobHolder jobHolder;

    final TaskMapper taskMapper;

    public SimpleTaskModifyReceiver(HotJobHolder jobHolder, TaskMapper taskMapper) {
        this.jobHolder = jobHolder;
        this.taskMapper = taskMapper;
    }

    @Override
    public void receive(List<ReportedTask> reportedTasks) {

        reportedTasks.forEach(reportedTask -> {
            if (reportedTask.getFailedReason() != null) {
                taskMapper.updateFailedReason(reportedTask.getId(), reportedTask.getFailedReason());
            }

            Task optionalTasks = jobHolder.getTask(reportedTask.getId());

            if (null == optionalTasks) {
                // TODO Update step and job status?
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
                    taskMapper.updateTaskStartedTimeIfNotSet(
                            reportedTask.getId(), new Date(reportedTask.getStartTimeMillis()));
                }
                if (reportedTask.getStopTimeMillis() != null) {
                    taskMapper.updateTaskFinishedTimeIfNotSet(
                            reportedTask.getId(), new Date(reportedTask.getStopTimeMillis()));
                }
                return;
            }
            if (reportedTask.getRetryCount() != null && reportedTask.getRetryCount() > 0) {
                optionalTasks.setRetryNum(reportedTask.getRetryCount());
                taskMapper.updateRetryNum(reportedTask.getId(), reportedTask.getRetryCount());
            }
            if (StringUtils.hasText(reportedTask.getIp())) {
                optionalTasks.setIp(reportedTask.getIp());
                taskMapper.updateIp(reportedTask.getId(), reportedTask.getIp());
            }
            if (reportedTask.status != TaskStatus.UNKNOWN) {
                // update time before status because only the updateStatus will trigger the watcher
                // TODO optimize the update time logic
                if (optionalTasks.getStartTime() == null) {
                    optionalTasks.setStartTime(reportedTask.getStartTimeMillis());
                }
                if (optionalTasks.getFinishTime() == null) {
                    optionalTasks.setFinishTime(reportedTask.getStopTimeMillis());
                }
                optionalTasks.updateStatus(reportedTask.getStatus());
            }
        });

    }
}
