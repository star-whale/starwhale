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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import ai.starwhale.mlops.domain.job.step.task.WatchableTask;
import ai.starwhale.mlops.domain.job.step.task.WatchableTaskFactory;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.schedule.TaskScheduler;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * load job to JobHolder as active state
 */
@Slf4j
@Service
public class JobScheduler {

    final HotJobHolder jobHolder;

    final WatchableTaskFactory watchableTaskFactory;

    final TaskScheduler taskScheduler;

    final TaskService taskService;

    public JobScheduler(HotJobHolder jobHolder,
                        WatchableTaskFactory watchableTaskFactory,
                        TaskScheduler taskScheduler,
                        TaskService taskService) {
        this.jobHolder = jobHolder;
        this.watchableTaskFactory = watchableTaskFactory;
        this.taskScheduler = taskScheduler;
        this.taskService = taskService;
    }

    public Job schedule(@NotNull Job job, Boolean resumePausedOrFailTasks) {
        //wrap task with watchers
        job.getSteps().forEach(step -> {
            List<Task> watchableTasks = watchableTaskFactory.wrapTasks(step.getTasks());
            step.setTasks(watchableTasks);
            if (resumePausedOrFailTasks) {
                resumeFrozenTasks(watchableTasks);
            }
        });
        // set job to memory after tasks are wrapped with watchers
        jobHolder.add(job);
        // schedule ready task after job is set to memory
        scheduleReadyTasks(
                job.getSteps().stream()
                        .map(Step::getTasks).flatMap(List::stream)
                        .filter(t -> t.getStatus() == TaskStatus.READY)
                        .collect(Collectors.toSet())
        );
        return job;
    }

    public void pause(Long jobId) {
        Job job = jobHolder.getJob(jobId);
        if (null == job) {
            throw new SwValidationException(SwValidationException.ValidSubject.JOB,
                    "Completed jobs cannot be paused.");
        }
        if (job.getStatus() != JobStatus.RUNNING) {
            throw new SwValidationException(SwValidationException.ValidSubject.JOB,
                    "Job's status is not RUNNING, can't be paused.");
        }
        List<Task> waitingTasks = job.tasks()
                .filter(task -> task.getStatus() == TaskStatus.CREATED)
                .collect(Collectors.toList());
        if (waitingTasks.isEmpty()) {
            return;
        }
        updateAndNotifyStatus(waitingTasks, TaskStatus.PAUSED);
    }

    public void cancel(Long jobId) {
        Job job = jobHolder.getJob(jobId);
        if (null == job) {
            throw new StarwhaleApiException(
                    new SwValidationException(
                            SwValidationException.ValidSubject.JOB, "Completed jobs cannot be canceled."),
                    HttpStatus.BAD_REQUEST);
        }
        if (job.getStatus() != JobStatus.RUNNING
                && job.getStatus() != JobStatus.PAUSED
                // there is a case that should try to cancel when job is fail
                && job.getStatus() != JobStatus.FAIL) {
            throw new SwValidationException(SwValidationException.ValidSubject.JOB,
                    "Job's status is not RUNNING/PAUSED, can't be canceled.");
        }
        List<Task> directlyCanceledTasks = job.tasks()
                .filter(task -> task.getStatus() == TaskStatus.READY
                    || task.getStatus() == TaskStatus.PREPARING
                    || task.getStatus() == TaskStatus.RUNNING)
                .collect(Collectors.toList());
        if (directlyCanceledTasks.isEmpty()) {
            return;
        }
        updateAndNotifyStatus(directlyCanceledTasks, TaskStatus.CANCELLING);
    }

    private void updateAndNotifyStatus(List<Task> waitingTasks, TaskStatus paused) {
        // first store to DB
        taskService.batchUpdateTaskStatus(waitingTasks, paused);

        CompletableFuture.runAsync(() -> {
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForPersist.class));
            waitingTasks.forEach(task -> task.updateStatus(paused));
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
        });
    }

    public void remove(Long jobId) {
        jobHolder.remove(jobId);
    }

    private void resumeFrozenTasks(List<Task> tasks) {
        tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PAUSED
                            || t.getStatus() == TaskStatus.FAIL
                            || t.getStatus() == TaskStatus.CANCELED)
                .forEach(t -> {
                    // FAIL -> ready is forbidden by status machine, so make it to CREATED at first
                    ((WatchableTask) t).unwrap().updateStatus(TaskStatus.CREATED);
                    t.updateStatus(TaskStatus.READY);
                });
    }

    /**
     * load READY tasks on start
     */
    void scheduleReadyTasks(Collection<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        taskScheduler.schedule(tasks);
    }

}
