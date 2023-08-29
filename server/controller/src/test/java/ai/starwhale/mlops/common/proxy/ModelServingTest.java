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

package ai.starwhale.mlops.common.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelServingTest {
    private ModelServingMapper modelServingMapper;
    private ModelServing modelServing;

    private TaskMapper taskMapper;

    private StepMapper stepMapper;

    @BeforeEach
    void setUp() {
        modelServingMapper = mock(ModelServingMapper.class);
        taskMapper = mock(TaskMapper.class);
        stepMapper = mock(StepMapper.class);
        modelServing = new ModelServing(modelServingMapper, taskMapper, stepMapper);
    }

    @Test
    void getPrefix() {
        var prefix = modelServing.getPrefix();
        assertEquals("model-serving", prefix);
    }

    @Test
    void getTarget() {
        long id = 1L;
        when(modelServingMapper.find(id)).thenReturn(ModelServingEntity.builder().jobId(1L).build());
        when(stepMapper.findByJobId(1L)).thenReturn(null);
        var target = modelServing.getTarget("1");
        Assertions.assertNull(target);

        when(stepMapper.findByJobId(1L)).thenReturn(List.of(StepEntity.builder().id(1L).build()));
        when(taskMapper.findByStepId(1L)).thenReturn(null);
        target = modelServing.getTarget("1");
        Assertions.assertNull(target);

        when(stepMapper.findByJobId(1L)).thenReturn(List.of(StepEntity.builder().id(1L).build()));
        when(taskMapper.findByStepId(1L)).thenReturn(List.of(TaskEntity.builder().build()));
        target = modelServing.getTarget("1");
        Assertions.assertNull(target);

        when(stepMapper.findByJobId(1L)).thenReturn(List.of(StepEntity.builder().id(1L).build()));
        when(taskMapper.findByStepId(1L)).thenReturn(List.of(TaskEntity.builder().ip("ip").build()));
        target = modelServing.getTarget("1");
        Assertions.assertEquals("http://ip:8080/", target);

    }
}
