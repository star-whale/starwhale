/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskStatusStage;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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
public class LivingTaskStatusMachineImpl implements LivingTaskStatusMachine {

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
    final ConcurrentHashMap<StagingTaskStatus, Set<Long>> taskStatusMap;

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

    public LivingTaskStatusMachineImpl(TaskMapper taskMapper, JobMapper jobMapper,
        TaskJobStatusHelper taskJobStatusHelper) {
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskJobStatusHelper = taskJobStatusHelper;
        taskIdMap = new ConcurrentHashMap<>();
        jobIdMap = new ConcurrentHashMap<>();
        taskStatusMap = new ConcurrentHashMap<>();
        jobTaskMap = new ConcurrentHashMap<>();
        toBePersistentTasks = new ConcurrentLinkedQueue<>();
        toBeCheckedJobs = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void adopt(Collection<Task> tasks, final StagingTaskStatus status) {
        tasks.parallelStream().forEach(task -> {
            updateCache(status, task);
        });
    }

    @Override
    @Transactional
    public void update(Collection<Task> livingTasks, StagingTaskStatus status) {
        if(null == livingTasks || livingTasks.isEmpty()){
            log.debug("empty tasks to be updated for status{}",status.getValue());
            return;
        }
        final Stream<Task> toBeUpdateStream = livingTasks.parallelStream().filter(task -> {
            final Task taskResident = taskIdMap.get(task.getId());
            if (null == taskResident) {
                log.debug("no resident task of id {}", task.getId());
                return true;
            }
            final boolean statusBeforeNewStatus = taskResident.getStatus().before(status);
            log.debug("task status change from {} to {} is valid? {}", taskResident.getStatus(),
                status, statusBeforeNewStatus);
            return statusBeforeNewStatus;
        });

        final List<Task> toBeUpdatedTasks = toBeUpdateStream
            .map(task -> getOrInsert(task))
            .peek(task -> {
                final Long jobId = task.getJob().getId();
                updateCache(status, task);
                toBeCheckedJobs.offer(jobId);
            })
            .collect(Collectors.toList());

        if (flushDB(status)) {
            persistTaskStatus(toBeUpdatedTasks);
        } else {
            toBePersistentTasks.addAll(toBeUpdatedTasks.parallelStream()
                .map(Task::getId)
                .collect(Collectors.toList()));
        }
    }

    @Override
    public Collection<Task> ofStatus(StagingTaskStatus taskStatus) {
        return safeGetTaskIdsFromStatus(taskStatus).stream().map(tskId->taskIdMap.get(tskId).deepCopy())
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Task> ofId(Long taskId) {
        return Optional.ofNullable(taskIdMap.get(taskId).deepCopy());
    }

    @Override
    public Collection<Task> ofJob(Long jobId) {
        return jobTaskMap.get(jobId).stream().map(tskId->taskIdMap.get(tskId).deepCopy())
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

    void persistTaskStatus(List<Task> tasks) {
        tasks.parallelStream().collect(Collectors.groupingBy(Task::getStatus))
            .forEach((taskStatus, taskList) -> taskMapper
                .updateTaskStatus(taskList.stream().map(Task::getId).collect(Collectors.toList()),
                    taskStatus.getValue()));
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
                return null != job && job.getStatus().before(desiredStatus);
            }).peek(jobId -> jobIdMap.get(jobId).setStatus(desiredStatus))
                .collect(Collectors.toList());
            if(null != toBeUpdated && !toBeUpdated.isEmpty()){
                jobMapper.updateJobStatus(toBeUpdated, desiredStatus.getValue());
            }

            if (desiredStatus == JobStatus.FINISHED) {
                removeFinishedJobTasks(jobids);
            }

        });
    }

    private void removeFinishedJobTasks(List<Long> jobids) {
        if(CollectionUtils.isEmpty(jobids)){
            return;
        }
        jobids.parallelStream().forEach(jid->{
            final Set<Long> toBeCleardTaskIds = jobTaskMap.get(jid);
            final Set<Long> finishedTasks = safeGetTaskIdsFromStatus(
                new StagingTaskStatus(TaskStatus.FINISHED,
                    TaskStatusStage.DONE));
            jobIdMap.remove(jid);
            jobTaskMap.remove(jid);
            toBeCleardTaskIds.parallelStream().forEach(tid->{
                taskIdMap.remove(tid);
                finishedTasks.remove(tid);
            });

        });

    }


    //easily lost statuses must be flush to db immediately
    private boolean flushDB(StagingTaskStatus status) {
        return status.getStatus().isFinalStatus()
            || status.getStage() != TaskStatusStage.INIT;
    }

    private void updateCache(StagingTaskStatus newStatus, Task task) {
        //update jobIdMap
        Long jobId = task.getJob().getId();
        getOrInsertJob(task, jobId);
        //update taskStatusMap
        Set<Long> taskIdsOfNewStatus = safeGetTaskIdsFromStatus(newStatus);
        taskIdsOfNewStatus.add(task.getId());
        StagingTaskStatus oldStatus = task.getStatus();
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

    private Job getOrInsertJob(Task task, Long jobId) {
        return jobIdMap.computeIfAbsent(jobId, k -> task.getJob());
    }

    private Set<Long> safeGetTaskIdsFromJob(Long jobId) {
        return jobTaskMap.computeIfAbsent(jobId,
            k -> Collections.synchronizedSet(new HashSet<>()));
    }

    private Set<Long> safeGetTaskIdsFromStatus(StagingTaskStatus oldStatus) {
        return taskStatusMap.computeIfAbsent(oldStatus, k -> Collections.synchronizedSet(new HashSet<>()));
    }

}
