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

package ai.starwhale.mlops.domain.job.cache;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.task.WatchableTask;
import ai.starwhale.mlops.domain.job.step.task.WatchableTaskFactory;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.schedule.TaskScheduler;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * load job to JobHolder as active state
 */
@Slf4j
@Service
public class JobLoader {

    final HotJobHolder jobHolder;

    final WatchableTaskFactory watchableTaskFactory;

    final TaskScheduler taskScheduler;

    public JobLoader(HotJobHolder jobHolder, WatchableTaskFactory watchableTaskFactory, TaskScheduler taskScheduler) {
        this.jobHolder = jobHolder;
        this.watchableTaskFactory = watchableTaskFactory;
        this.taskScheduler = taskScheduler;
    }

    public Job load(@NotNull Job job, Boolean resumePausedOrFailTasks) {
        //wrap task with watchers
        job.getSteps().forEach(step -> {
            List<Task> watchableTasks = watchableTaskFactory.wrapTasks(step.getTasks());
            step.setTasks(watchableTasks);
            if (resumePausedOrFailTasks) {
                resumeFrozenTasks(watchableTasks);
            }
        });
        //set job to memory after tasks are wrapped with watchers
        jobHolder.adopt(job);
        //schedule ready task after job is set to memory
        scheduleReadyTasks(
                job.getSteps().stream()
                        .map(Step::getTasks).flatMap(List::stream)
                        .filter(t -> t.getStatus() == TaskStatus.READY)
                        .collect(Collectors.toSet())
        );
        return job;

    }

    private void resumeFrozenTasks(List<Task> tasks) {
        tasks.parallelStream().filter(t -> t.getStatus() == TaskStatus.PAUSED
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
