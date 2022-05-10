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

import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.task.bo.Task.StatusUnModifiableTask;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.*;

import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * an implementation of LivingTaskStatusMachine
 */
@Slf4j
@Service
public class LivingTaskCacheImpl implements LivingTaskCache {

    /**
     * contains hot tasks
     * key: task.id
     * value: task.identity
     */
    final ConcurrentHashMap<Long, Task> taskIdMap;

    /**
     * contains hot jobs
     * key: task.id
     * value: task.identity
     */
    final ConcurrentHashMap<Long, Job> jobIdMap;

    /**
     * key: task.status
     * value: task.id
     */
    final ConcurrentHashMap<ai.starwhale.mlops.domain.task.status.TaskStatus, Set<Long>> taskStatusMap;

    /**
     * key: task.jobId
     * value: task.id
     */
    final ConcurrentHashMap<Long, Set<Long>> jobTaskMap;

    /**
     * task.id
     */
    final ConcurrentLinkedQueue<Long> toBePersistentTasks;

    /**
     * task.jobId
     */
    final ConcurrentLinkedQueue<Long> toBeCheckedJobs;

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final TaskJobStatusHelper taskJobStatusHelper;

    final TaskStatusMachine taskStatusMachine;

    final JobStatusMachine jobStatusMachine;

    final SWTaskScheduler swTaskScheduler;

    final static Set<ai.starwhale.mlops.domain.task.status.TaskStatus> easyLostStatuses = Set.of(SUCCESS, FAIL,
        CANCELED,ASSIGNING,CANCELLING);

    public LivingTaskCacheImpl(TaskMapper taskMapper, JobMapper jobMapper,
        TaskJobStatusHelper taskJobStatusHelper,
        TaskStatusMachine taskStatusMachine,
        JobStatusMachine jobStatusMachine,
        SWTaskScheduler swTaskScheduler) {
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskJobStatusHelper = taskJobStatusHelper;
        this.taskStatusMachine = taskStatusMachine;
        this.jobStatusMachine = jobStatusMachine;
        this.swTaskScheduler = swTaskScheduler;
        taskIdMap = new ConcurrentHashMap<>();
        jobIdMap = new ConcurrentHashMap<>();
        taskStatusMap = new ConcurrentHashMap<>();
        jobTaskMap = new ConcurrentHashMap<>();
        toBePersistentTasks = new ConcurrentLinkedQueue<>();
        toBeCheckedJobs = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void adopt(Collection<Task> tasks, final TaskStatus status) {
        tasks.parallelStream().map(task -> {
            if(task instanceof StatusUnModifiableTask){
                StatusUnModifiableTask statusUnModifiableTask = (StatusUnModifiableTask) task;
                return statusUnModifiableTask.getOTask();
            }
            return task;
        }).forEach(task -> {
            updateCache(status, task);
        });
    }

    @Override
    @Transactional
    public void update(Collection<Long> livingTasks, TaskStatus newStatus) {
        if(null == livingTasks || livingTasks.isEmpty()){
            log.debug("empty tasks to be updated for newStatus{}",newStatus);
            return;
        }
        final Stream<Long> toBeUpdateStream = livingTasks.parallelStream().filter(task -> {
            final Task taskResident = taskIdMap.get(task);
            if (null == taskResident) {
                log.debug("no resident task of id {}", task);
                return false;
            }
            final boolean statusBeforeNewStatus = taskStatusMachine.couldTransfer(taskResident.getStatus(),newStatus);
            log.debug("task newStatus change from {} to {} is valid? {}", taskResident.getStatus(),
                newStatus, statusBeforeNewStatus);
            return statusBeforeNewStatus;
        });

        final List<Task> toBeUpdatedTasks = toBeUpdateStream
            .map(taskId -> taskIdMap.get(taskId))
            .peek(task -> {
                final Long jobId = task.getJob().getId();
                updateCache(newStatus, task);
                toBeCheckedJobs.offer(jobId);
            })
            .collect(Collectors.toList());

        if (easyLost(newStatus)) {
            persistTaskStatus(toBeUpdatedTasks);
        } else {
            toBePersistentTasks.addAll(toBeUpdatedTasks.parallelStream()
                .map(Task::getId)
                .collect(Collectors.toList()));
        }
    }

    /**
     *
     * @param taskStatus
     * @return StatusUnModifiableTasks are returned.
     */
    @Override
    public Collection<Task> ofStatus(TaskStatus taskStatus) {
        return safeGetTaskIdsFromStatus(taskStatus).stream().map(tskId->taskIdMap.get(tskId).statusUnModifiable())
            .collect(Collectors.toList());
    }

    /**
     *
     * @param taskId
     * @return StatusUnModifiableTask is returned.
     */
    @Override
    public Optional<Task> ofId(Long taskId) {
        return Optional.ofNullable(taskIdMap.get(taskId)).map(t->t.statusUnModifiable());
    }

    /**
     *
     * @param jobId
     * @return StatusUnModifiableTasks are returned.
     */
    @Override
    public Collection<Task> ofJob(Long jobId) {
        return jobTaskMap.get(jobId).stream().map(tskId->taskIdMap.get(tskId).statusUnModifiable())
            .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 1000)
    public void doPersist() {
        persistTaskStatus(drainToSet(toBePersistentTasks));
        persistJobStatus(drainToSet(toBeCheckedJobs));
    }

    /**
     * compensation for the failure case when all tasks of one job are final status but updating job status failed
     */
    @Scheduled(fixedDelay = 1000 * 60)
    public void checkAllJobs() {
        persistJobStatus(jobIdMap.keySet());
    }

    Set<Long> drainToSet(
        ConcurrentLinkedQueue<Long> queue) {
        Set<Long> jobSet = new HashSet<>();
        Long poll;
        while (true){
            poll = queue.poll();
            if(null == poll){
                break;
            }
            jobSet.add(poll);
        }
        return jobSet;
    }

    void persistTaskStatus(Set<Long> taskIds){
        if(CollectionUtils.isEmpty(taskIds)){
            return;
        }
        persistTaskStatus(taskIds.parallelStream().map(id->taskIdMap.get(id)).collect(Collectors.toList()));
    }


    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;

    void persistTaskStatus(List<Task> tasks) {
        tasks.parallelStream().collect(Collectors.groupingBy(Task::getStatus))
            .forEach((taskStatus, taskList) ->
                BatchOperateHelper.doBatch(taskList
                    , taskStatus
                    , (tsks, status) -> taskMapper.updateTaskStatus(
                        tsks.stream().map(Task::getId).collect(Collectors.toList()),
                        status)
                    , MAX_BATCH_SIZE));
    }

    /**
     * change job status triggered by living task status change
     */
    void persistJobStatus(Set<Long> toBeCheckedJobs) {
        if(CollectionUtils.isEmpty(toBeCheckedJobs)){
            return;
        }
        final Map<JobStatus, List<Long>> jobDesiredStatusMap = toBeCheckedJobs.parallelStream()
            .collect(Collectors.groupingBy((jobid -> taskJobStatusHelper.desiredJobStatus(this.ofJob(jobid)))));
        jobDesiredStatusMap.forEach((desiredStatus, jobids) -> {
            //filter these job who's current status is before desired status
            final List<Long> toBeUpdated = jobids.parallelStream().filter(jid -> {
                final Job job = jobIdMap.get(jid);
                return null != job;
            }).peek(jobId -> jobIdMap.get(jobId).setStatus(desiredStatus))
                .collect(Collectors.toList());
            if(null != toBeUpdated && !toBeUpdated.isEmpty()){
                jobMapper.updateJobStatus(toBeUpdated, desiredStatus);
            }

            if(desiredStatus == JobStatus.FAIL){
                swTaskScheduler.stopSchedule(jobids.parallelStream().map(jid->jobTaskMap.get(jid)).flatMap(Collection::stream).collect(
                    Collectors.toList()));
            }
            if (jobStatusMachine.isFinal(desiredStatus)) {
                jobMapper.updateJobFinishedTime(jobids, Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime());
                removeFinishedJobTasks(jobids);
            }

        });
    }

    private void removeFinishedJobTasks(List<Long> jobids) {
        jobids.parallelStream().forEach(jid->{
            final Set<Long> toBeClearedTaskIds = jobTaskMap.get(jid);
            jobIdMap.remove(jid);
            jobTaskMap.remove(jid);
            taskStatusMap.forEach((k,tskIds)->{
                tskIds.removeAll(toBeClearedTaskIds);
            });

            toBeClearedTaskIds.parallelStream().forEach(tid->{
                taskIdMap.remove(tid);
            });

        });

    }

    private void updateCache(TaskStatus newStatus, Task task) {
        //update jobIdMap
        Long jobId = task.getJob().getId();
        getOrInsertJob(task.getJob());
        //update taskStatusMap
        Set<Long> taskIdsOfNewStatus = safeGetTaskIdsFromStatus(newStatus);
        taskIdsOfNewStatus.add(task.getId());
        TaskStatus oldStatus = task.getStatus();
        if(!newStatus.equals(oldStatus)){
            Set<Long> taskIdsOfOldStatus = safeGetTaskIdsFromStatus(oldStatus);
            taskIdsOfOldStatus.remove(task.getId());
        }
        //update jobTaskMap
        Set<Long> taskIdsOfJob = safeGetTaskIdsFromJob(jobId);
        if(!taskIdsOfJob.contains(task.getId())){
            taskIdsOfJob.add(task.getId());
        }
        //update taskIdMap
        task.setStatus(newStatus);
        taskIdMap.put(task.getId(),task);
    }


    private Task getOrInsert(Task task) {
        return taskIdMap.computeIfAbsent(task.getId(), k -> task);
    }

    private Job getOrInsertJob(Job job) {
        return jobIdMap.computeIfAbsent(job.getId(), k -> job);
    }

    private Set<Long> safeGetTaskIdsFromJob(Long jobId) {
        return jobTaskMap.computeIfAbsent(jobId,
            k -> Collections.synchronizedSet(new HashSet<>()));
    }

    private Set<Long> safeGetTaskIdsFromStatus(TaskStatus oldStatus) {
        return taskStatusMap.computeIfAbsent(oldStatus, k -> Collections.synchronizedSet(new HashSet<>()));
    }

    private boolean easyLost(TaskStatus status){
        return easyLostStatuses.contains(status);
    }

}
