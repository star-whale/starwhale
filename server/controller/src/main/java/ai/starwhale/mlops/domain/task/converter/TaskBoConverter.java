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

package ai.starwhale.mlops.domain.task.converter;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.api.protocol.report.resp.TaskRequest;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.system.agent.AgentConverter;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import cn.hutool.json.JSONUtil;
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

    final AgentConverter agentConverter;

    final LocalDateTimeConvertor localDateTimeConvertor;

    public TaskBoConverter(AgentConverter agentConverter,
        ai.starwhale.mlops.common.LocalDateTimeConvertor localDateTimeConvertor) {
        this.agentConverter = agentConverter;
        this.localDateTimeConvertor = localDateTimeConvertor;
    }


    public List<Task> fromTaskEntity(List<TaskEntity> entities, Step step){
        return entities.parallelStream().map(entity -> transformTask(step, entity)).collect(Collectors.toList());
    }

    public Task transformTask(Step step, TaskEntity entity) {
        Task task = Task.builder()
            .id(entity.getId())
            .step(step)
            .status(entity.getTaskStatus())
            .uuid(entity.getTaskUuid())
            .resultRootPath(new ResultPath(entity.getOutputPath()))
            .taskRequest(JSONUtil.toBean(entity.getTaskRequest(), TaskRequest.class))
            .build();
        task.setStartTime(localDateTimeConvertor.convert(entity.getStartedTime()));
        task.setFinishTime(localDateTimeConvertor.convert(entity.getFinishedTime()));
        return task;
    }

}
