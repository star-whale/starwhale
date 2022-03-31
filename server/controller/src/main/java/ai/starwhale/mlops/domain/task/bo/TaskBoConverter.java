/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.swds.index.SWDSBlockSerializer;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * convert task objects
 */
@Slf4j
public class TaskBoConverter {

    SWDSBlockSerializer swdsBlockSerializer;

    public Task fromTaskEntity(TaskEntity entity){
        return Task.builder()
            .id(entity.getId())
            .jobId(entity.getJobId())
            .status(TaskStatus.from(entity.getTaskStatus()))
            .resultPaths(entity.getResultPath())
            .swdsBlocks(entity.getSwdsBlocks())
            .build();
    }

    public List<TaskTrigger> toTaskTrigger(List<TaskEntity> tasks, Job job){
        return tasks.parallelStream()
            .map(this::fromTaskEntity)
            .map(t->{
            try {
                return TaskTrigger.builder().task(t)
                    .imageId(job.getJobRuntime().getBaseImage())
                    .swdsBlocks(swdsBlockSerializer.fromString(t.getSwdsBlocks()))
                    .swModelPackage(job.getSwmp()).build();
            } catch (JsonProcessingException e) {
                log.error("read swds blocks from db failed ",e);
                throw new SWValidationException(ValidSubject.TASK);
            }
        }).collect(Collectors.toList());
    }

    public List<TaskCommand> toTaskCommand(List<TaskEntity> tasks) {
        return tasks.parallelStream()
            .map(t -> new TaskCommand(TaskCommand.CommandType.from(TaskStatus.from(t.getTaskStatus())), fromTaskEntity(t)))
            .collect(Collectors.toList());
    }

}
