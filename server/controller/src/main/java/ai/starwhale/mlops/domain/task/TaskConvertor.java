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

import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.system.AgentConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class TaskConvertor implements Convertor<TaskEntity, TaskVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Resource
    private AgentConvertor agentConvertor;

    @Override
    public TaskVO convert(TaskEntity entity) throws ConvertException {
        if(entity == null) {
            return TaskVO.empty();
        }
        return TaskVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .uuid(entity.getTaskUuid())
            .taskStatus(entity.getTaskStatus())
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .agent(agentConvertor.convert(entity.getAgent()))
            .build();
    }

    @Override
    public TaskEntity revert(TaskVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "TaskVO");
        return TaskEntity.builder()
            .taskStatus(vo.getTaskStatus())
            .taskUuid(vo.getUuid())
            .build();
    }
}
