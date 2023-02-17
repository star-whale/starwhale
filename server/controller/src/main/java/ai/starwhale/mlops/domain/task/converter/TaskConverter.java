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

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.Converter;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TaskConverter implements Converter<TaskEntity, TaskVo> {

    private final IdConverter idConvertor;

    public TaskConverter(IdConverter idConvertor) {
        this.idConvertor = idConvertor;
    }

    @Override
    public TaskVo convert(TaskEntity entity) throws ConvertException {
        if (entity == null) {
            return TaskVo.empty();
        }
        return TaskVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .uuid(entity.getTaskUuid())
                .taskStatus(entity.getTaskStatus())
                .retryNum(entity.getRetryNum())
                .createdTime(entity.getStartedTime().getTime())
                .build();
    }

    @Override
    public TaskEntity revert(TaskVo vo) throws ConvertException {
        Objects.requireNonNull(vo, "TaskVo");
        return TaskEntity.builder()
                .taskStatus(vo.getTaskStatus())
                .retryNum(vo.getRetryNum())
                .taskUuid(vo.getUuid())
                .build();
    }
}
