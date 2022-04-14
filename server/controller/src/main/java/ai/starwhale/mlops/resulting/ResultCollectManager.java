/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.api.protocol.resulting.EvaluationResult;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.JobMapper;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskMapper;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskStatusStage;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * coordinate collectors of jobs
 */
@Slf4j
@Service
public class ResultCollectManager {

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final CollectorFinder collectorFinder;

    final LivingTaskStatusMachine livingTaskStatusMachine;

    final Map<Long,ResultCollector> resultCollectors = new ConcurrentHashMap<>();

    final StorageAccessService storageAccessService;

    public ResultCollectManager(TaskMapper taskMapper,
        JobMapper jobMapper, CollectorFinder collectorFinder,
        LivingTaskStatusMachine livingTaskStatusMachine, StorageAccessService storageAccessService) {
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.collectorFinder = collectorFinder;
        this.livingTaskStatusMachine = livingTaskStatusMachine;
        this.storageAccessService = storageAccessService;
    }

    public EvaluationResult resultOfJob(Long jobId){
        final ResultCollector resultCollector = getResultCollector(jobId);
        return new EvaluationResult(resultCollector.getClass().getName(),resultCollector.collect());
    }

    /**
     * collection done jobs will be persist to DB by TaskStatusMachine
     */
    @Scheduled(fixedDelay = 1000)
    public void removeFinishedJobs(){
        List<Long> collectDoneJobs = resultCollectors.keySet().parallelStream().filter(jobId -> {
            JobEntity jobEntity = jobMapper.findJobById(jobId);
            return jobEntity.getJobStatus() == JobStatus.FINISHED.getValue()
                || jobEntity.getJobStatus() == JobStatus.CANCELED.getValue()
                || jobEntity.getJobStatus() == JobStatus.EXIT_ERROR.getValue();
        }).peek(jobId->{
            try {
                resultCollectors.get(jobId).dump();
            } catch (IOException e) {
                log.error("dumping result of job {} failed",jobId,e);
                jobMapper.updateJobStatus(List.of(jobId),JobStatus.EXIT_ERROR.getValue());
            }
        })
            .collect(Collectors.toList());

        collectDoneJobs.parallelStream().forEach(doneJob->resultCollectors.remove(doneJob));
    }

    @Scheduled(fixedDelay = 1000)
    public void onTaskFinished(){
        final Collection<Task> finishedTasks = livingTaskStatusMachine.ofStatus(new StagingTaskStatus(TaskStatus.FINISHED));
        livingTaskStatusMachine.update(finishedTasks,new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.DOING));
        finishedTasks.parallelStream()
            .forEach(taskEntity->{
                ResultCollector collector;
                final Long taskEntityId = taskEntity.getId();
                try{
                    collector = getResultCollectorFromCache(taskEntity.getJob().getId());
                }catch (SWValidationException e){
                    log.error("no result collector found for task {}",taskEntityId);
                    livingTaskStatusMachine.update(List.of(taskEntity),new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.FAILED));
                    return;
                }

                final String resultLabelPath = taskEntity.getResultPaths();
                final Stream<String> resultLabels;
                try {
                    resultLabels = storageAccessService.list(resultLabelPath);
                } catch (IOException e) {
                    log.error("listing inference results for task failed {}",taskEntityId,e);
                    livingTaskStatusMachine.update(List.of(taskEntity),new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.FAILED));
                    return;
                }
                resultLabels.forEach((labelComparePath)->{
                    try(final InputStream labelIS = storageAccessService.get(labelComparePath)){
                        collector.feed(labelIS);
                    }catch (IOException e){
                        log.error("reading labels failed for task {}",taskEntityId,e);
                        livingTaskStatusMachine.update(List.of(taskEntity),new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.FAILED));
                    }
                });
                livingTaskStatusMachine.update(finishedTasks,new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.DONE));
            });

    }

    ResultCollector getResultCollectorFromCache(Long jobId) throws SWValidationException{
        return resultCollectors
            .computeIfAbsent(jobId,
                jid -> getResultCollector(jid));
    }

    private ResultCollector getResultCollector(Long jid) {
        return collectorFinder.findCollector(jid).orElseThrow(()-> new SWValidationException(
            ValidSubject.JOB));
    }

    @PreDestroy
    public void exitHook() {
        log.debug("system exited!!");
        resultCollectors.entrySet().parallelStream().forEach(entry -> {
            final ResultCollector collector = entry.getValue();
            final List<Long> doingCollectTasks = taskMapper.listTasks(entry.getKey())
                .parallelStream().filter(task -> task.getTaskStatus() == new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.DOING).getValue())
                .map(TaskEntity::getId)
                .collect(Collectors.toList());
            try {
                collector.dump();
            } catch (IOException e) {
                //rollback taskStatus to init
                log.error("collector dump failed {} {}", collector.getClass(),collector.getIdentity());
                taskMapper.updateTaskStatus(doingCollectTasks,new StagingTaskStatus(TaskStatus.FINISHED).getValue());

            }
        });
    }



}
