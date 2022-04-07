/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.JobMapper;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.swds.index.SWDSBlockSerializer;
import ai.starwhale.mlops.domain.system.Agent;
import ai.starwhale.mlops.domain.task.TaskMapper;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * convert task objects
 */
@Slf4j
@Service
public class TaskBoConverter {

    final SWDSBlockSerializer swdsBlockSerializer;

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final JobBoConverter jobBoConverter;

    public TaskBoConverter(SWDSBlockSerializer swdsBlockSerializer, TaskMapper taskMapper, JobMapper jobMapper, JobBoConverter jobBoConverter) {
        this.swdsBlockSerializer = swdsBlockSerializer;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.jobBoConverter = jobBoConverter;
    }

    /**
     * heavy opt
     * @param id id of a task
     * @return task
     */
    public Task fromId(Long id){
        final TaskEntity entity = taskMapper.findTaskById(id);
        final JobEntity jobById = jobMapper.findJobById(entity.getJobId());
        return buildTask(jobBoConverter.fromEntity(jobById),entity);
    }

    public List<Task> fromTaskEntity(List<TaskEntity> entities,Job job){
        return entities.parallelStream().map(entity -> buildTask(job, entity)).collect(Collectors.toList());

    }

    public Task buildTask(Job job, TaskEntity entity) {
        try {
            return Task.builder()
                .id(entity.getId())
                .job(job)
                .agent(Agent.fromEntity(entity.getAgent()))
                .status(StagingTaskStatus.from(entity.getTaskStatus()))
                .resultPaths(entity.getResultPath())
                .swdsBlocks(swdsBlockSerializer.fromString(entity.getSwdsBlocks()))
                .build();
        } catch (JsonProcessingException e) {
            log.error("read swds blocks from db failed ",e);
            throw new SWValidationException(ValidSubject.TASK);
        }
    }

    public List<TaskTrigger> toTaskTrigger(List<Task> tasks){
        return tasks.parallelStream()
            .map(this::toTaskTrigger).collect(Collectors.toList());
    }

    public TaskTrigger toTaskTrigger(Task t){
        return TaskTrigger.builder()
            .id(t.getId())
            .imageId(t.getJob().getJobRuntime().getBaseImage())
            .resultPath(t.getResultPaths())
            .swdsBlocks(t.getSwdsBlocks())
            .swModelPackage(t.getJob().getSwmp()).build();
    }

    public List<TaskCommand> toTaskCommand(List<Task> tasks) {
        return tasks.parallelStream()
            .map(this::toTaskCommand)
            .collect(Collectors.toList());
    }

    public TaskCommand toTaskCommand(Task task) {
        return new TaskCommand(TaskCommand.CommandType.from(task.getStatus()), task);
    }

}
