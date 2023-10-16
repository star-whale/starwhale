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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.job.ExposedLinkVo;
import ai.starwhale.mlops.api.protocol.run.RunVo;
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.step.ExposedType;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.run.RunService;
import ai.starwhale.mlops.domain.task.converter.TaskConverter;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SwProcessException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskConverterTest {

    private StepMapper stepMapper;
    private TaskConverter taskConvertor;
    private WebServerInTask webServerInTask;
    private JobSpecParser jobSpecParser;

    private RunService runService;

    @BeforeEach
    public void setup() {
        stepMapper = mock(StepMapper.class);
        jobSpecParser = new JobSpecParser();
        when(stepMapper.findById(anyLong())).thenReturn(new StepEntity() {
            {
                setName("ppl");
            }
        });
        webServerInTask = mock(WebServerInTask.class);
        runService = mock(RunService.class);
        when(runService.runOfTask(any())).thenReturn(List.of(new RunVo() {
            {
                setId(3L);
            }
        }));
        taskConvertor = new TaskConverter(
                new IdConverter(), stepMapper, 8000, webServerInTask, jobSpecParser, runService
        );
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
                // this can not happen in real world (with the status RUNNING) but for test
                .finishedTime(new Date(12345679L))
                .taskUuid("uuid")
                .taskStatus(TaskStatus.RUNNING)
                .retryNum(1)
                .devWay(DevWay.VS_CODE)
                .ip("127.0.0.1")
                .build();
        when(webServerInTask.generateGatewayUrl(1L, "127.0.0.1", 8000)).thenReturn("/gateway/task/1/8000/");
        TaskVo taskVo = taskConvertor.convert(taskEntity);
        Assertions.assertEquals(taskVo.getTaskStatus(), taskEntity.getTaskStatus());
        Assertions.assertEquals(taskVo.getUuid(), taskEntity.getTaskUuid());
        Assertions.assertEquals(taskVo.getFinishedTime(), taskEntity.getFinishedTime().getTime());
        Assertions.assertEquals(taskVo.getStartedTime(), taskEntity.getStartedTime().getTime());
        Assertions.assertEquals(taskVo.getStepName(), "ppl");
        Assertions.assertEquals(taskVo.getRetryNum(), taskEntity.getRetryNum());
        Assertions.assertEquals(3L, taskVo.getRuns().get(0).getId());

        var expectedExposedLink = ExposedLinkVo.builder()
                .type(ExposedType.DEV_MODE)
                .name("VS_CODE")
                .link("/gateway/task/1/8000/")
                .build();
        assertThat(taskVo.getExposedLinks(), containsInAnyOrder(expectedExposedLink));

        when(stepMapper.findById(anyLong())).thenReturn(new StepEntity() {
            {
                setName("ppl");
                setOriginJson("{\"expose\": 8080, \"name\": \"foo\"}");
            }
        });
        when(webServerInTask.generateGatewayUrl(1L, "127.0.0.1", 8080)).thenReturn("/gateway/task/1/8080/");

        taskVo = taskConvertor.convert(taskEntity);
        var theWebHandlerLink = ExposedLinkVo.builder()
                .type(ExposedType.WEB_HANDLER)
                .name("foo")
                .link("/gateway/task/1/8080/")
                .build();

        assertThat(taskVo.getExposedLinks(), containsInAnyOrder(expectedExposedLink, theWebHandlerLink));

        // test not running
        taskEntity.setTaskStatus(TaskStatus.SUCCESS);
        taskVo = taskConvertor.convert(taskEntity);
        Assertions.assertNull(taskVo.getExposedLinks());

        // test no ip (should restore the status to running)
        taskEntity.setTaskStatus(TaskStatus.RUNNING);
        taskEntity.setIp("");
        taskVo = taskConvertor.convert(taskEntity);
        Assertions.assertNull(taskVo.getExposedLinks());
    }

}
