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

package ai.starwhale.mlops.domain.ft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.ft.FineTuneCreateRequest;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneMapper;
import ai.starwhale.mlops.domain.ft.po.FineTuneEntity;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.bo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class FineTuneAppServiceTest {

    JobCreator jobCreator;

    FineTuneMapper fineTuneMapper;

    JobMapper jobMapper;

    JobSpecParser jobSpecParser;

    ModelDao modelDao;

    DatasetDao datasetDao;

    FineTuneAppService fineTuneAppService;

    @BeforeEach
    public void setup() {
        jobCreator = mock(JobCreator.class);
        fineTuneMapper = mock(FineTuneMapper.class);
        jobMapper = mock(JobMapper.class);
        jobSpecParser = mock(JobSpecParser.class);
        modelDao = mock(ModelDao.class);
        datasetDao = mock(DatasetDao.class);
        fineTuneAppService = new FineTuneAppService(
                jobCreator,
                fineTuneMapper,
                jobMapper,
                jobSpecParser,
                modelDao,
                "instanceuri",
                datasetDao
        );
    }

    @Test
    void createFt() throws JsonProcessingException {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((FineTuneEntity) args[0]).setId(123L);
                return null; // void method, so return null
            }
        }).when(fineTuneMapper).add(any());
        when(jobCreator.createJob(any())).thenReturn(Job.builder().id(22L).build());

        FineTuneCreateRequest request = new FineTuneCreateRequest();
        request.setStepSpecOverWrites("aaa");
        request.setEvalDatasetVersionIds(List.of(1L));
        when(datasetDao.getDatasetVersion(anyLong())).thenReturn(DatasetVersion.builder().projectId(22L).datasetName(
                "dsn").versionName("dsv").build());
        when(jobSpecParser.parseAndFlattenStepFromYaml(any())).thenReturn(List.of(StepSpec.builder().build()));
        fineTuneAppService.createFineTune(1L, Project.builder().build(), request, User.builder().build());

        verify(fineTuneMapper).updateJobId(123L, 22L);

    }

    @Test
    void listFt() {
        when(fineTuneMapper.list(anyLong())).thenReturn(List.of(FineTuneEntity.builder().jobId(1L).build()));
        when(jobMapper.findJobById(1L)).thenReturn(JobEntity.builder().build());
        Assertions.assertEquals(1, fineTuneAppService.list(1L, 1, 1).getSize());
    }

    @Test
    void evalFt() {
    }

    @Test
    void releaseFt() {
    }
}