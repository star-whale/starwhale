/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * an implementation of LivingTaskStatusMachine
 */
@Slf4j
public class LivingTaskStatusMachineImpl implements LivingTaskStatusMachine {

    /**
     * key: task.id
     * value: task.identity
     */
    ConcurrentHashMap<Long, Task> taskIdMap;

    /**
     * key: task.status
     * value: task.id
     */
    ConcurrentHashMap<TaskStatus, List<Long>> taskStatusMap;

    /**
     * key: task.jobId
     * value: task.id
     */
    ConcurrentHashMap<Long, List<Long>> jobTaskMap;

    /**
     * task.id
     */
    ConcurrentLinkedQueue<Long> toBePersistentTasks;

    /**
     * task.jobId
     */
    ConcurrentLinkedQueue<Long> toBeCheckedJobs;

    final int persistBatchSize;

    public LivingTaskStatusMachineImpl(int persistBatchSize) {
        this.persistBatchSize = persistBatchSize;
        taskIdMap = new ConcurrentHashMap<>();
        taskStatusMap = new ConcurrentHashMap<>();
        jobTaskMap = new ConcurrentHashMap<>();
        toBePersistentTasks = new ConcurrentLinkedQueue<>();
        toBeCheckedJobs = new ConcurrentLinkedQueue<>();
    }


    @Override
    public void adopt(Collection<Task> livingTasks, final TaskStatus status) {
        //watch status change validation
        //update to taskStatusMap & taskIdMap
        //if in transaction context save immediately or add to toBePersistent
        boolean inTransactionContext = false;
        try {
            TransactionAspectSupport
                .currentTransactionStatus();
            inTransactionContext = true;
        } catch (NoTransactionException e) {
            log.debug("no transaction context in call of status {} ", status);
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
            .map(task -> taskIdMap.computeIfAbsent(task.getId(), k -> task))
            .peek(task -> {
                task.setStatus(status);
                taskStatusMap.computeIfAbsent(status,k-> Collections.synchronizedList(new LinkedList<>()))
                    .add(task.getId());
                jobTaskMap.computeIfAbsent(task.getJobId(),k->Collections.synchronizedList(new LinkedList<>()))
                    .add(task.getId());
                toBeCheckedJobs.offer(task.getJobId());
            })
            .collect(Collectors.toList());


        if (inTransactionContext) {
            persistTaskStatus(toBeUpdatedTasks);
        } else {
            toBePersistentTasks.addAll(toBeUpdatedTasks.parallelStream()
                .map(Task::getId)
                .collect(Collectors.toList()));
        }

    }

    @Override
    public Collection<Task> ofStatus(TaskStatus taskStatus) {
        return taskStatusMap.get(taskStatus).stream().map(taskIdMap::get)
            .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 1000)
    public void doPersist() {
        if (toBePersistentTasks.size() > persistBatchSize) {
            //if FINISHED OR ERROR is persist ,remove from taskIdMap
        }
        Set<Long> jobSet = new HashSet<>();
        Long poll;
        while (true){
            poll = toBeCheckedJobs.poll();
            if(null == poll){
                break;
            }
            jobSet.add(poll);
        }

        //update job status
    }

    void persistTaskStatus(List<Task> tasks) {
        //TODO
    }

    /**
     * change job status triggered by living task status change
     * SPLIT        ->  SCHEDULED
     * SCHEDULED    ->  FINISHED
     * TO_CANCEL    ->  CANCELED
     *
     */
    void checkJob(Set<Long> toBeCheckedJobs){

    }

    void persistJobStatus(List<Long> jobIds, JobStatus jobStatus){
        //TODO
    }
}
