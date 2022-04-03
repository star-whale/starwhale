/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobMapper;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskStatusStage;
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
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;

/**
 * an implementation of LivingTaskStatusMachine
 */
@Slf4j
public class LivingTaskStatusMachineImpl implements LivingTaskStatusMachine {

    /**
     * contains hot tasks
     * key: task.id
     * value: task.identity
     */
    ConcurrentHashMap<Long, Task> taskIdMap;

    /**
     * contains hot jobs
     * key: task.id
     * value: task.identity
     */
    ConcurrentHashMap<Long, Job> jobIdMap;

    /**
     * key: task.status
     * value: task.id
     */
    ConcurrentHashMap<StagingTaskStatus, List<Long>> taskStatusMap;

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

    TaskMapper taskMapper;

    JobMapper jobMapper;

    public LivingTaskStatusMachineImpl() {
        taskIdMap = new ConcurrentHashMap<>();
        jobIdMap = new ConcurrentHashMap<>();
        taskStatusMap = new ConcurrentHashMap<>();
        jobTaskMap = new ConcurrentHashMap<>();
        toBePersistentTasks = new ConcurrentLinkedQueue<>();
        toBeCheckedJobs = new ConcurrentLinkedQueue<>();
    }


    @Override
    public void adopt(Collection<Task> livingTasks, final StagingTaskStatus status) {
        //watch status change validation
        //update to taskStatusMap & taskIdMap
        //if in transaction context save immediately or add to toBePersistent
        boolean inTransactionContext = false;
        try {
            TransactionAspectSupport.currentTransactionStatus();
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
                final Long jobId = task.getJob().getId();
                jobTaskMap.computeIfAbsent(jobId,k->Collections.synchronizedList(new LinkedList<>()))
                    .add(task.getId());
                toBeCheckedJobs.offer(jobId);
                jobIdMap.computeIfAbsent(jobId, k ->task.getJob());
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
    public Collection<Task> ofStatus(StagingTaskStatus taskStatus) {
        return taskStatusMap.get(taskStatus).stream().map(taskIdMap::get)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Task> ofId(Long taskId) {
        return Optional.ofNullable(taskIdMap.get(taskId));
    }

    @Scheduled(fixedDelay = 1000)
    public void doPersist() {
        persistTaskStatus(drainToSet(toBePersistentTasks));
        persistJobStatus(drainToSet(toBeCheckedJobs));
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
        final Map<JobStatus, List<Long>> jobDesiredStatusMap = toBeCheckedJobs.parallelStream()
            .collect(Collectors.groupingBy((jobid -> {
                final List<Long> taskIds = jobTaskMap.get(jobid);
                final JobStatus desiredJobStatuses = taskIds.parallelStream()
                    .map(taskId -> taskIdMap.get(taskId).getStatus())
                    .reduce(JobStatus.FINISHED, (jobStatus, taskStatus) -> {
                            if (taskStatus.getDesiredJobStatus().before(jobStatus)) {
                                jobStatus = taskStatus.getDesiredJobStatus();
                            }
                            return jobStatus;
                        }
                        , (js1, js2) -> js1.before(js2) ? js1 : js2);
                return desiredJobStatuses;
            })));
        jobDesiredStatusMap.forEach((desiredStatus, jobids) -> {
            //filter these job who's current status is before desired status
            final List<Long> toBeUpdated = jobids.parallelStream().filter(jid -> {
                final Job job = jobIdMap.get(jid);
                return null != job && job.getStatus().before(desiredStatus);
            }).peek(jobId -> jobIdMap.get(jobId).setStatus(desiredStatus))
                .collect(Collectors.toList());
            jobMapper.updateJobStatus(toBeUpdated, desiredStatus.getValue());

            if (desiredStatus == JobStatus.FINISHED) {
                //todo(renyanda) when to remove
                removeFinishedJobTasks(jobids);
            }

        });
    }

    private void removeFinishedJobTasks(List<Long> jobids) {
        if(CollectionUtils.isEmpty(jobids)){
            return;
        }
        jobids.parallelStream().forEach(jid->{
            final List<Long> toBeCleardTaskIds = jobTaskMap.get(jid);
            final List<Long> finishedTasks = taskStatusMap.get(new StagingTaskStatus(TaskStatus.FINISHED,
                TaskStatusStage.DONE));
            jobIdMap.remove(jid);
            toBeCleardTaskIds.parallelStream().forEach(tid->{
                taskIdMap.remove(tid);
                finishedTasks.remove(tid);
            });

        });

    }

}
