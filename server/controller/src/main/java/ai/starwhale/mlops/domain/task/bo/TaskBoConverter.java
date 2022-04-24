/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.swds.index.SWDSBlockSerializer;
import ai.starwhale.mlops.domain.system.Agent;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.ppl.PPLRequest;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
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
        return transformTask(jobBoConverter.fromEntity(jobById),entity);
    }

    public List<Task> fromTaskEntity(List<TaskEntity> entities,Job job){
        return entities.parallelStream().map(entity -> transformTask(job, entity)).collect(Collectors.toList());
    }

    public Task transformTask(Job job, TaskEntity entity) {
        try {
            TaskRequest taskRequest;
            TaskType taskType = entity.getTaskType();
            switch (taskType){
                case PPL:
                    taskRequest = new PPLRequest(swdsBlockSerializer.fromString(entity.getTaskRequest())) ;
                    break;
                case CMP:
                    taskRequest = new CMPRequest(entity.getTaskRequest()) ;
                    break;
                case UNKNOWN:
                default:
                    throw new SWValidationException(ValidSubject.TASK).tip("unknown task type "+entity.getTaskType());
            }
            return Task.builder()
                .id(entity.getId())
                .job(job)
                .agent(Agent.fromEntity(entity.getAgent()))
                .status(entity.getTaskStatus())
                .resultDir(entity.getResultPath())
                .uuid(entity.getTaskUuid())
                .taskRequest(taskRequest)
                .taskType(taskType)
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
        switch (t.getTaskType()){
            case PPL:
                return TaskTrigger.builder()
                    .id(t.getId())
                    .imageId(t.getJob().getJobRuntime().getBaseImage())
                    .resultPath(t.getResultDir())
                    .swdsBlocks(((PPLRequest)t.getTaskRequest()).getSwdsBlocks())
                    .deviceAmount(t.getJob().getJobRuntime().getDeviceAmount())
                    .deviceClass(t.getJob().getJobRuntime().getDeviceClass())
                    .taskType(t.getTaskType())
                    .swModelPackage(t.getJob().getSwmp()).build();
            case CMP:
                return TaskTrigger.builder()
                    .id(t.getId())
                    .resultPath(t.getResultDir())
                    .cmpInputFilePaths(((CMPRequest)t.getTaskRequest()).getPplResultPaths())
                    .taskType(t.getTaskType())
                    .deviceAmount(1)
                    .deviceClass(Clazz.CPU)
                    .imageId(t.getJob().getJobRuntime().getBaseImage())
                    .swModelPackage(t.getJob().getSwmp()).build();
            case UNKNOWN:
            default:
                throw new SWValidationException(ValidSubject.TASK).tip("task type unknown "+t.getTaskType());
        }


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
