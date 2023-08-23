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

package ai.starwhale.mlops.domain.job;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeVersionConverter;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuntimeSuggestionServiceTest {
    @Test
    public void testGetSuggestions() {
        var runtimeVersionMapper = mock(RuntimeVersionMapper.class);
        var runtimeVersionConverter = mock(RuntimeVersionConverter.class);
        var jobMapper = mock(JobMapper.class);
        var modelServingMapper = mock(ModelServingMapper.class);
        var runtimeSuggestionService = new RuntimeSuggestionService(
                runtimeVersionMapper,
                runtimeVersionConverter,
                jobMapper,
                modelServingMapper
        );

        final var projectId = 1L;
        final var modelVersionId = 2L;

        // no online eval or eval jobs
        var runtime = RuntimeVersionEntity.builder().id(3L).runtimeId(4L).build();
        when(runtimeVersionMapper.findLatestByProjectId(1L, 1)).thenReturn(List.of(runtime));
        var runtimeVo = RuntimeVersionVo.builder().id("2").build();
        when(runtimeVersionConverter.convert(runtime)).thenReturn(runtimeVo);
        var suggestions = runtimeSuggestionService.getSuggestions(projectId, null);
        Assertions.assertEquals(List.of(runtimeVo), suggestions);

        // model version id not null
        suggestions = runtimeSuggestionService.getSuggestions(projectId, modelVersionId);
        Assertions.assertEquals(List.of(runtimeVo), suggestions);

        final var evalRuntimeId1 = 7L;
        final var evalRuntimeId2 = 8L;

        var runtime1 = RuntimeVersionEntity.builder().id(evalRuntimeId1).runtimeId(1L).build();
        var runtime2 = RuntimeVersionEntity.builder().id(evalRuntimeId2).runtimeId(1L).build();
        when(runtimeVersionMapper.find(evalRuntimeId1)).thenReturn(runtime1);
        when(runtimeVersionMapper.find(evalRuntimeId2)).thenReturn(runtime2);
        var runtimeVo2 = RuntimeVersionVo.builder().id("8").image("image of 8").build();
        when(runtimeVersionConverter.convert(runtime2)).thenReturn(runtimeVo2);

        var jobs = List.of(
                JobEntity.builder()
                        .id(1L)
                        .projectId(projectId)
                        .modelVersionId(modelVersionId)
                        .runtimeVersionId(evalRuntimeId1)
                        .build(),
                JobEntity.builder()
                        .id(2L)
                        .projectId(projectId)
                        .modelVersionId(modelVersionId)
                        .runtimeVersionId(evalRuntimeId2)
                        .build()
        );
        // eval job exists and no online eval job
        when(jobMapper.listUserJobs(projectId, modelVersionId)).thenReturn(jobs);
        suggestions = runtimeSuggestionService.getSuggestions(projectId, modelVersionId);
        // get the max id of the used runtime version
        Assertions.assertEquals(List.of(runtimeVo2), suggestions);

        var modelServingEntity1 = ModelServingEntity.builder()
                .id(10L).lastVisitTime(new Date(100)).runtimeVersionId(9L).build();
        var modelServingEntity2 = ModelServingEntity.builder().id(11L).lastVisitTime(new Date(10)).build();
        var runtime9 = RuntimeVersionEntity.builder().id(9L).runtimeId(1L).build();
        when(runtimeVersionMapper.find(9L)).thenReturn(runtime9);
        var runtimeVo9 = RuntimeVersionVo.builder().id("9").image("image of 9").build();
        when(runtimeVersionConverter.convert(runtime9)).thenReturn(runtimeVo9);
        // eval job and online eval job exists
        when(modelServingMapper.list(projectId, modelVersionId, null, null)).thenReturn(
                new ArrayList<>(List.of(modelServingEntity1, modelServingEntity2))
        );

        suggestions = runtimeSuggestionService.getSuggestions(projectId, modelVersionId);
        // get the last visit time of the used runtime version
        Assertions.assertEquals(List.of(runtimeVo9), suggestions);
    }
}
