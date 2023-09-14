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

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * the processor for every report from Agent
 */
@Slf4j
@Service
public class SimpleTaskReportReceiver implements TaskReportReceiver, RollingUpdateStatusListener {

    final HotJobHolder jobHolder;

    final TaskMapper taskMapper;

    final SwTaskScheduler taskScheduler;
    final JobBoConverter jobBoConverter;

    private volatile boolean primaryInstance;

    public SimpleTaskReportReceiver(HotJobHolder jobHolder, TaskMapper taskMapper, @Lazy SwTaskScheduler taskScheduler,
                                    JobBoConverter jobBoConverter
    ) {
        this.jobHolder = jobHolder;
        this.taskMapper = taskMapper;
        this.taskScheduler = taskScheduler;
        this.jobBoConverter = jobBoConverter;
    }

    @Override
    public void receive(List<ReportedTask> reportedTasks) {
        if (!primaryInstance) {
            log.info("server is upgrading and i'm not the primary instance, abandon all reported info");
            return;
        }

        reportedTasks.forEach(reportedTask -> {
            if (reportedTask.getFailedReason() != null) {
                taskMapper.updateFailedReason(reportedTask.getId(), reportedTask.getFailedReason());
            }

            Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(reportedTask.getId()));
            Task inMemoryTask = null;
            if (!CollectionUtils.isEmpty(optionalTasks)) {
                inMemoryTask = optionalTasks.iterator().next();
            }

            if (inMemoryTask == null) {
                Job job = jobBoConverter.fromTaskId(reportedTask.getId());
                if (null == job) {
                    log.error("bad data from scheduler: no job for reported task {}", reportedTask.getId());
                    return;
                }
                taskScheduler.stop(Set.of(job.getTask(reportedTask.getId())));
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
            // prevent all the task status update before the task resume
            if (inMemoryTask.getGeneration() != null) {
                if (reportedTask.getGeneration() == null) {
                    log.debug("no generation from report {}, ignore", reportedTask.getId());
                    return;
                }
                if (reportedTask.getGeneration() < inMemoryTask.getGeneration()) {
                    log.debug("generation from report {} {} is less than cached generation {}, ignore",
                            reportedTask.getId(), reportedTask.getGeneration(), inMemoryTask.getGeneration());
                    return;
                }
            }

            if (reportedTask.getRetryCount() != null && reportedTask.getRetryCount() > 0) {
                inMemoryTask.setRetryNum(reportedTask.getRetryCount());
                taskMapper.updateRetryNum(reportedTask.getId(), reportedTask.getRetryCount());
            }
            if (StringUtils.hasText(reportedTask.getIp())) {
                inMemoryTask.setIp(reportedTask.getIp());
                taskMapper.updateIp(reportedTask.getId(), reportedTask.getIp());
            }
            if (reportedTask.status != TaskStatus.UNKNOWN) {
                // update time before status because only the updateStatus will trigger the watcher
                // TODO optimize the update time logic
                if (inMemoryTask.getStartTime() == null) {
                    inMemoryTask.setStartTime(reportedTask.getStartTimeMillis());
                }
                if (inMemoryTask.getFinishTime() == null) {
                    inMemoryTask.setFinishTime(reportedTask.getStopTimeMillis());
                }
                inMemoryTask.updateStatus(reportedTask.getStatus());
            }
        });

    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) {
        // As the status report is idempotent, and we could not suffer the loss for status
        // there shall be a small overlap instead of a small gap between the primary instances
        if (status == ServerInstanceStatus.READY_UP) {
            primaryInstance = false;
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        // As the status report is idempotent, and we could not suffer the loss for status
        // there shall be a small overlap instead of a small gap between the primary instances
        if (status == ServerInstanceStatus.READY_DOWN) {
            primaryInstance = true;
        }
    }
}
