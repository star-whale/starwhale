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

package ai.starwhale.mlops.domain.task;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.task.converter.TaskConverter;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SwProcessException;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskConverterTest {

    private StepMapper stepMapper;
    private TaskConverter taskConvertor;
    private FeaturesProperties featuresProperties;

    @BeforeEach
    public void setup() {
        stepMapper = mock(StepMapper.class);
        when(stepMapper.findById(anyLong())).thenReturn(new StepEntity() {
            {
                setName("ppl");
            }
        });
        featuresProperties = mock(FeaturesProperties.class);
        taskConvertor = new TaskConverter(new IdConverter(), stepMapper, 8000, featuresProperties);
    }


    @Test
    public void testNullInput() {
        Assertions.assertNull(taskConvertor.convert(null));
    }

    @Test
    public void testInvalidData() {
        when(stepMapper.findById(anyLong())).thenReturn(null);
        Assertions.assertThrowsExactly(
                SwProcessException.class, () -> taskConvertor.convert(new TaskEntity() {
                    {
                        setId(1L);
                    }
                })
        );
    }

    @Test
    public void testValidData() {
        when(stepMapper.findById(anyLong())).thenReturn(new StepEntity() {
            {
                setName("ppl");
            }
        });
        TaskEntity taskEntity = TaskEntity.builder()
                .id(1L)
                .stepId(1L)
                .startedTime(new Date(12345678L))
                .finishedTime(new Date(12345679L))
                .taskUuid("uuid")
                .taskStatus(TaskStatus.SUCCESS)
                .retryNum(1)
                .devWay(DevWay.VS_CODE)
                .ip("127.0.0.1")
                .build();
        when(featuresProperties.isJobProxyEnabled()).thenReturn(true);
        TaskVo taskVo = taskConvertor.convert(taskEntity);
        Assertions.assertEquals(taskVo.getTaskStatus(), taskEntity.getTaskStatus());
        Assertions.assertEquals(taskVo.getUuid(), taskEntity.getTaskUuid());
        Assertions.assertEquals(taskVo.getFinishedTime(), taskEntity.getFinishedTime().getTime());
        Assertions.assertEquals(taskVo.getStartedTime(), taskEntity.getStartedTime().getTime());
        Assertions.assertEquals(taskVo.getStepName(), "ppl");
        Assertions.assertEquals(taskVo.getRetryNum(), taskEntity.getRetryNum());
        Assertions.assertEquals(taskVo.getDevUrl(), "/gateway/task/1/8000/");

        when(featuresProperties.isJobProxyEnabled()).thenReturn(false);
        taskVo = taskConvertor.convert(taskEntity);
        Assertions.assertEquals(taskVo.getTaskStatus(), taskEntity.getTaskStatus());
        Assertions.assertEquals(taskVo.getUuid(), taskEntity.getTaskUuid());
        Assertions.assertEquals(taskVo.getFinishedTime(), taskEntity.getFinishedTime().getTime());
        Assertions.assertEquals(taskVo.getStartedTime(), taskEntity.getStartedTime().getTime());
        Assertions.assertEquals(taskVo.getStepName(), "ppl");
        Assertions.assertEquals(taskVo.getRetryNum(), taskEntity.getRetryNum());
        Assertions.assertEquals(taskVo.getDevUrl(), "http://127.0.0.1:8000");

        taskEntity.setIp("");
        taskVo = taskConvertor.convert(taskEntity);
        Assertions.assertNull(taskVo.getDevUrl());
    }

}
