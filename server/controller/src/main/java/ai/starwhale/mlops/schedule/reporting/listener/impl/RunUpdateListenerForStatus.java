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

package ai.starwhale.mlops.schedule.reporting.listener.impl;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.WatchableTask;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.reporting.listener.RunUpdateListener;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RunUpdateListenerForStatus implements RunUpdateListener {

    final HotJobHolder hotJobHolder;

    final SwTaskScheduler swTaskScheduler;

    final TaskStatusMachine taskStatusMachine;
    final TaskMapper taskMapper;

    final Integer backOffLimit;

    public RunUpdateListenerForStatus(
            HotJobHolder hotJobHolder,
            SwTaskScheduler swTaskScheduler,
            TaskStatusMachine taskStatusMachine,
            TaskMapper taskMapper,
            @Value("${sw.scheduler.backOffLimit}") int backOffLimit
    ) {
        this.hotJobHolder = hotJobHolder;
        this.swTaskScheduler = swTaskScheduler;
        this.taskStatusMachine = taskStatusMachine;
        this.taskMapper = taskMapper;
        this.backOffLimit = backOffLimit;
    }

    @Override
    public void onRunUpdate(Run run) {
        Long taskId = run.getTaskId();
        Task task = hotJobHolder.taskWithId(run.getTaskId());
        if (task == null) {
            log.warn("run of detached task reported, taskId: {}, runId: {}", taskId, run.getId());
            return;
        }
        Run currentRun = task.getCurrentRun();
        if (null == currentRun) {
            log.warn("detached run reported, taskId: {}, runId: {}", taskId, run.getId());
            return;
        }
        if (!run.getId().equals(currentRun.getId())) {
            log.warn(
                    "illegal run reported, taskId: {}, reported run: {}, current run {}",
                    taskId,
                    run.getId(),
                    currentRun.getId()
            );
            return;
        }
        if (run.getStatus() == currentRun.getStatus()) {
            log.info(
                    "run status not changed, taskId: {}, runId: {}, status: {}",
                    taskId,
                    run.getId(),
                    run.getStatus()
            );
            return;
        }
        currentRun.setStatus(run.getStatus());
        TaskStatus taskNewStatus;
        Integer retryNum = task.getRetryNum();
        retryNum = null == retryNum ? 0 : retryNum;
        Integer userRetryLimit = task.getStep().getSpec().getBackOffLimit();
        Integer backOffLimit = userRetryLimit == null ? this.backOffLimit : userRetryLimit;
        if (
                run.getStatus() == RunStatus.FAILED
                        && retryNum < backOffLimit
                        && task.getStatus() != TaskStatus.CANCELLING
        ) {
            retryNum = retryNum + 1;
            task.setRetryNum(retryNum);
            taskMapper.updateRetryNum(taskId, retryNum);
            taskNewStatus = TaskStatus.RETRYING;
            if (task instanceof WatchableTask) {
                Task ot = ((WatchableTask) task).unwrap();
                ot.updateStatus(TaskStatus.READY);
            }
        } else {
            taskNewStatus = taskStatusMachine.transfer(task.getStatus(), run.getStatus());
        }

        if (run.getFailedReason() != null) {
            taskMapper.updateFailedReason(run.getTaskId(), run.getFailedReason());
        }
        if (run.getStartTime() != null) {
            taskMapper.updateTaskStartedTimeIfNotSet(run.getTaskId(), new Date(run.getStartTime()));
        }
        if (run.getFinishTime() != null) {
            taskMapper.updateTaskFinishedTimeIfNotSet(run.getTaskId(), new Date(run.getFinishTime()));
        }
        if (run.getIp() != null) {
            taskMapper.updateIp(run.getTaskId(), run.getIp());
        }
        task.updateStatus(taskNewStatus);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
