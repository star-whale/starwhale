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
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.log.TaskLogSaver;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(6)
public class TaskWatcherForSchedule implements TaskStatusChangeWatcher {

    final SwTaskScheduler swTaskScheduler;

    final TaskStatusMachine taskStatusMachine;

    final Long deletionDelayMilliseconds;

    final DelayQueue<TaskToDelete> taskToDeletes;

    private final TaskLogSaver taskLogSaver;

    final TaskReportReceiver taskReportReceiver;

    public TaskWatcherForSchedule(
            SwTaskScheduler swTaskScheduler,
            TaskStatusMachine taskStatusMachine,
            @Value("${sw.task.deletion-delay-minutes}") Long deletionDelayMinutes,
            TaskLogSaver taskLogSaver, TaskReportReceiver taskReportReceiver) {
        this.swTaskScheduler = swTaskScheduler;
        this.taskStatusMachine = taskStatusMachine;
        this.deletionDelayMilliseconds = TimeUnit.MILLISECONDS.convert(deletionDelayMinutes, TimeUnit.MINUTES);
        this.taskLogSaver = taskLogSaver;
        this.taskReportReceiver = taskReportReceiver;
        this.taskToDeletes = new DelayQueue<>();
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus oldStatus) {
        if (task.getStatus() == TaskStatus.READY) {
            log.debug("task status changed to ready id: {} oldStatus: {}, scheduled", task.getId(), oldStatus);
            swTaskScheduler.schedule(List.of(task), taskReportReceiver);
        } else if (task.getStatus() == TaskStatus.CANCELLING || task.getStatus() == TaskStatus.PAUSED) {
            taskLogSaver.saveLog(task);
            swTaskScheduler.stop(List.of(task));
            log.debug("task status changed to {} with id: {} newStatus: {}, stop scheduled immediately",
                    task.getStatus(),
                    task.getId(), task.getStatus());
        } else if (taskStatusMachine.isFinal(task.getStatus())) {
            log.debug("task status changed to {} with id: {} newStatus: {}, stop scheduled in delayed queue",
                    task.getStatus(),
                    task.getId(), task.getStatus());
            taskLogSaver.saveLog(task);
            if (deletionDelayMilliseconds <= 0) {
                swTaskScheduler.stop(List.of(task));
            } else {
                addToDeleteQueue(task);
            }
        } else {
            //do nothing
            log.debug("task {} of status {} do nothing with scheduler ", task.getId(), task.getStatus());
        }
    }

    private void addToDeleteQueue(Task task) {
        var deleteTime = System.currentTimeMillis() + deletionDelayMilliseconds;
        taskToDeletes.put(new TaskToDelete(task, deleteTime));
        log.debug("add task {} to delete queue, delete time {}", task.getId(), deleteTime);
    }

    @Scheduled(fixedDelay = 30000)
    public void processTaskDeletion() {
        var toDelete = taskToDeletes.poll();
        List<Task> tasks = new ArrayList<>();
        while (toDelete != null) {
            tasks.add(toDelete.getTask());
            log.debug("delete task {}", toDelete.getTask().getId());
            toDelete = taskToDeletes.poll();
        }
        swTaskScheduler.stop(tasks);
    }

    public boolean hasTaskToDelete() {
        return taskToDeletes.size() > 0;
    }

    static class TaskToDelete implements Delayed {

        private final Task task;

        private final Long deleteTime;

        public TaskToDelete(Task task, Long deleteTime) {
            this.task = task;
            this.deleteTime = deleteTime;
        }

        public Task getTask() {
            return task;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return unit.convert(deleteTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            long diffMillis = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
            diffMillis = Math.min(diffMillis, 1);
            diffMillis = Math.max(diffMillis, -1);
            return (int) diffMillis;
        }
    }
}
