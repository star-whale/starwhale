/**
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

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * loading tasks
 */
@Service
public class LivingTaskLoader implements CommandLineRunner {

    final LivingTaskCache livingTaskCache;

    final SWTaskScheduler swTaskScheduler;

    final CommandingTasksChecker commandingTasksChecker;

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final TaskBoConverter taskBoConverter;

    final JobBoConverter jobBoConverter;

    final JobStatusMachine jobStatusMachine;

    public LivingTaskLoader(LivingTaskCache livingTaskCache,
        SWTaskScheduler swTaskScheduler, CommandingTasksChecker commandingTasksChecker,
        TaskMapper taskMapper, JobMapper jobMapper, TaskBoConverter taskBoConverter,
        JobBoConverter jobBoConverter,
        JobStatusMachine jobStatusMachine) {
        this.livingTaskCache = livingTaskCache;
        this.swTaskScheduler = swTaskScheduler;
        this.commandingTasksChecker = commandingTasksChecker;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskBoConverter = taskBoConverter;
        this.jobBoConverter = jobBoConverter;
        this.jobStatusMachine = jobStatusMachine;
    }

    /**
     * load tasks that are not FINISHED or ERROR into mem
     */
    void loadLivingTasks(){
        //load living tasks and assign them to livingTaskStatusMachine
        Stream<TaskEntity> taskStream = livingTasksFromDB();
        final Map<Long, List<TaskEntity>> collectJob = taskStream.parallel()
            .collect(Collectors.groupingBy(TaskEntity::getJobId));
        final Map<TaskStatus, List<Task>> collectStatus = collectJob.entrySet().parallelStream()
            .map(entry -> {
                final JobEntity job = jobMapper.findJobById(entry.getKey());
                return taskBoConverter
                    .fromTaskEntity(entry.getValue(), jobBoConverter.fromEntity(job));
            }).flatMap(Collection::stream)
            .collect(Collectors.groupingBy(Task::getStatus));

        collectStatus.entrySet().parallelStream()
            .forEach(entry -> livingTaskCache.adopt(entry.getValue(), entry.getKey()));

        scheduleCreatedTasks(collectStatus.get(TaskStatus.CREATED));
        checkCommandingTasks(collectStatus.get(TaskStatus.ASSIGNING));
        checkCommandingTasks(collectStatus.get(TaskStatus.CANCELLING));
    }

    /**
     * @return tasks of jobs that are not FINISHED neither ERROR neither ERROR
     */
    private Stream<TaskEntity> livingTasksFromDB() {
        List<JobStatus> hotJobStatuses = Arrays.asList(JobStatus.values())
            .parallelStream()
            .filter(jobStatus -> !jobStatusMachine.isFinal(jobStatus))
            .collect(Collectors.toList());
        return jobMapper.findJobByStatusIn(hotJobStatuses)
            .parallelStream()
            .map(jobEntity -> taskMapper.listTasks(jobEntity.getId()))
            .flatMap(Collection::stream);
    }


    /**
     * load CREATED tasks on start
     */
    void scheduleCreatedTasks(List<Task> tasks){
        if (null == tasks) {
            return;
        }
        tasks.parallelStream()
            .collect(Collectors.groupingBy(task -> task.getJob().getJobRuntime().getDeviceClass()))
            .forEach((deviceClass, taskList) ->
                swTaskScheduler.adoptTasks(taskList, deviceClass));
    }

    /**
     * load commanding tasks on start
     */
    void checkCommandingTasks(List<Task> tasks){
        if(null == tasks){
            return;
        }
        tasks.parallelStream()
            .collect(Collectors.groupingBy(Task::getAgent))
            .forEach((agent, taskList) -> commandingTasksChecker
                .onTaskCommanding(taskBoConverter.toTaskCommand(taskList),agent));

    }

    @Override
    public void run(String... args) throws Exception {
        loadLivingTasks();
    }
}
