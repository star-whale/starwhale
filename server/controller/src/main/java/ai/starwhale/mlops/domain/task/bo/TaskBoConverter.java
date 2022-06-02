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

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.swds.index.SWDSBlockSerializer;
import ai.starwhale.mlops.domain.system.agent.AgentConverter;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.bo.ppl.PPLRequest;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
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

    final AgentConverter agentConverter;

    final ResultPathConverter resultPathConverter;

    public TaskBoConverter(SWDSBlockSerializer swdsBlockSerializer, TaskMapper taskMapper,
        JobMapper jobMapper, JobBoConverter jobBoConverter,
        AgentConverter agentConverter, ResultPathConverter resultPathConverter) {
        this.swdsBlockSerializer = swdsBlockSerializer;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.jobBoConverter = jobBoConverter;
        this.agentConverter = agentConverter;
        this.resultPathConverter = resultPathConverter;
    }


    public List<Task> fromTaskEntity(List<TaskEntity> entities, Step step){
        return entities.parallelStream().map(entity -> transformTask(step, entity)).collect(Collectors.toList());
    }

    public Task transformTask(Step step, TaskEntity entity) {
        try {
            TaskRequest taskRequest;
            TaskType taskType = entity.getTaskType();
            switch (taskType){
                case PPL:
                    taskRequest = new PPLRequest(swdsBlockSerializer.fromString(entity.getTaskRequest())) ;
                    break;
                case CMP:
                    if(null != entity.getTaskRequest()){
                        taskRequest = new CMPRequest(entity.getTaskRequest()) ;
                    }else {
                        taskRequest = null;
                    }

                    break;
                case UNKNOWN:
                default:
                    throw new SWValidationException(ValidSubject.TASK).tip("unknown task type "+entity.getTaskType());
            }
            return Task.builder()
                .id(entity.getId())
                .step(step)
                .agent(agentConverter.fromEntity(entity.getAgent()))
                .status(entity.getTaskStatus())
                .resultRootPath(new ResultPath(entity.getResultPath()))
                .uuid(entity.getTaskUuid())
                .taskRequest(taskRequest)
                .taskType(taskType)
                .build();
        } catch (JsonProcessingException e) {
            log.error("read swds blocks or resultPath from db failed ",e);
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
                    .imageId(t.getStep().getJob().getJobRuntime().getBaseImage())
                    .resultPath(t.getResultRootPath())
                    .swdsBlocks(((PPLRequest)t.getTaskRequest()).getSwdsBlocks())
                    .deviceAmount(t.getStep().getJob().getJobRuntime().getDeviceAmount())
                    .deviceClass(t.getStep().getJob().getJobRuntime().getDeviceClass())
                    .taskType(t.getTaskType())
                    .swModelPackage(t.getStep().getJob().getSwmp()).build();
            case CMP:
                return TaskTrigger.builder()
                    .id(t.getId())
                    .resultPath(t.getResultRootPath())
                    .cmpInputFilePaths(((CMPRequest)t.getTaskRequest()).getPplResultPaths())
                    .taskType(t.getTaskType())
                    .deviceAmount(1)
                    .deviceClass(Clazz.CPU)
                    .imageId(t.getStep().getJob().getJobRuntime().getBaseImage())
                    .swModelPackage(t.getStep().getJob().getSwmp()).build();
            case UNKNOWN:
            default:
                throw new SWValidationException(ValidSubject.TASK).tip("task type unknown "+t.getTaskType());
        }


    }

    public List<TaskCommand> toTaskCommand(List<Task> tasks) {
        return tasks.parallelStream()
            .map(this::toTaskCommand)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public TaskCommand toTaskCommand(Task task) {
        CommandType commandType = CommandType.from(task.getStatus());
        if(commandType == CommandType.UNKNOWN){
            log.info("task already dispatched id: {} current status: {}",task.getId(),task.getStatus());
            return null;
        }
        return new TaskCommand(commandType, task);
    }

}
