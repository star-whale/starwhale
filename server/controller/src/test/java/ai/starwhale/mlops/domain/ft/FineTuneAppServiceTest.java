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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.evaluation.storage.EvaluationRepo;
import ai.starwhale.mlops.domain.event.EventService;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneMapper;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneSpaceMapper;
import ai.starwhale.mlops.domain.ft.po.FineTuneEntity;
import ai.starwhale.mlops.domain.ft.po.FineTuneSpaceEntity;
import ai.starwhale.mlops.domain.ft.vo.FineTuneVo;
import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.converter.UserJobConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.step.Env;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.step.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FineTuneAppServiceTest {

    JobCreator jobCreator;

    FineTuneMapper fineTuneMapper;
    FineTuneSpaceMapper fineTuneSpaceMapper;

    JobMapper jobMapper;
    JobStatusMachine jobStatusMachine = new JobStatusMachine();

    JobSpecParser jobSpecParser;

    ModelDao modelDao;

    DatasetDao datasetDao;

    FineTuneAppService fineTuneAppService;

    FeaturesProperties featuresProperties;

    ModelService modelService;
    User creator = User.builder().build();
    JobConverter jobConverter;

    EvaluationRepo evaluationRepo;

    @BeforeEach
    public void setup() {
        jobCreator = mock(JobCreator.class);
        fineTuneMapper = mock(FineTuneMapper.class);
        jobMapper = mock(JobMapper.class);
        jobSpecParser = mock(JobSpecParser.class);
        modelDao = mock(ModelDao.class);
        datasetDao = mock(DatasetDao.class);
        UserJobConverter userJobConverter = mock(UserJobConverter.class);
        when(userJobConverter.convert(any(), any())).thenReturn(UserJobCreateRequest.builder().build());
        fineTuneSpaceMapper = mock(FineTuneSpaceMapper.class);
        featuresProperties = mock(FeaturesProperties.class);
        when(featuresProperties.isFineTuneEnabled()).thenReturn(true);
        modelService = mock(ModelService.class);
        jobConverter = mock(JobConverter.class);
        evaluationRepo = mock(EvaluationRepo.class);
        fineTuneAppService = new FineTuneAppService(
                featuresProperties,
                jobCreator,
                fineTuneMapper,
                jobMapper,
                jobStatusMachine,
                jobSpecParser,
                new IdConverter(),
                modelDao,
                "instanceuri",
                datasetDao,
                fineTuneSpaceMapper,
                userJobConverter,
                mock(EventService.class),
                jobConverter,
                modelService,
                mock(DatasetService.class),
                evaluationRepo
        );
    }

    @Test
    void createFt() throws JsonProcessingException {
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((FineTuneEntity) args[0]).setId(123L);
            return null; // void method, so return null
        }).when(fineTuneMapper).add(any());
        when(jobCreator.createJob(any())).thenReturn(Job.builder().id(22L).build());

        var request = new JobRequest();
        request.setStepSpecOverWrites("aaa");
        request.setValidationDatasetVersionIds(List.of("1"));
        request.setModelVersionId("1");
        when(datasetDao.getDatasetVersion(anyString())).thenReturn(DatasetVersion.builder().projectId(22L).datasetName(
                "dsn").versionName("dsv").build());
        when(jobSpecParser.parseAndFlattenStepFromYaml(any()))
                .thenReturn(List.of(StepSpec.builder()
                                            .env(List.of(
                                                    new Env(
                                                            "dupK",
                                                            "v"
                                                    ),
                                                    new Env(
                                                            "dupK",
                                                            "v2"
                                                    )
                                            ))
                                            .build())
                );
        when(modelDao.findVersionById(anyLong())).thenReturn(ModelVersionEntity.builder().build());
        fineTuneAppService.createFineTune("1", 1L, request);

        verify(fineTuneMapper).updateJobId(123L, 22L);

    }

    @Test
    void listFt() {
        when(fineTuneMapper.list(anyLong())).thenReturn(List.of(FineTuneEntity.builder().jobId(1L).build()));
        when(jobMapper.findJobById(1L)).thenReturn(JobEntity.builder().build());
        assertEquals(1, fineTuneAppService.list(1L, 1, 1).getSize());
    }

    @Test
    void listFtOnlineEval() {
        when(jobMapper.listBizJobs(1L, BizType.FINE_TUNE.name(), "1", JobType.ONLINE_EVAL.name(), null))
                .thenReturn(List.of(JobEntity.builder().id(1L).build(), JobEntity.builder().id(2L).build()));
        when(jobConverter.convert(any())).thenReturn(JobVo.builder().build());
        assertEquals(2, fineTuneAppService.listOnlineEval(1L, 1L).size());
    }

    @Test
    void ftInfo() {
        when(fineTuneMapper.findById(anyLong(), anyLong())).thenReturn(FineTuneEntity.builder().jobId(1L).build());
        when(jobMapper.findJobById(1L)).thenReturn(JobEntity.builder().build());
        JobVo jobVo = JobVo.builder().build();
        when(jobConverter.convert(any())).thenReturn(jobVo);
        FineTuneVo fineTuneVo = fineTuneAppService.ftInfo(1L, 1L);
        assertEquals(jobVo, fineTuneVo.getJob());
    }

    @Test
    void ftImport() {
        when(jobMapper.findJobByUuids(anyList(), anyLong())).thenReturn(List.of(
                JobEntity.builder().jobUuid("uuid1").jobStatus(JobStatus.RUNNING).build(),
                JobEntity.builder().jobUuid("uuid2").jobStatus(JobStatus.SUCCESS).build(),
                JobEntity.builder().jobUuid("uuid3").jobStatus(JobStatus.FAIL).build()
        ));
        when(evaluationRepo.migration(
                "project/1/eval/summary",
                List.of("uuid2", "uuid3"),
                "project/1/ftspace/1/eval/summary")
        ).thenReturn(2);

        var result = fineTuneAppService.importEvalFromCommon(1L, 1L, List.of("uuid1", "uuid2", "uuid3"));
        assertEquals(2, result.getSuccess());
        assertEquals(1, result.getFail());
        verify(evaluationRepo, times(1)).migration(anyString(), eq(List.of("uuid2", "uuid3")), anyString());
    }

    @Test
    void ftExport() {
        when(jobMapper.findJobByUuids(anyList(), anyLong())).thenReturn(List.of(
                JobEntity.builder()
                        .jobUuid("uuid1")
                        .jobStatus(JobStatus.RUNNING)
                        .modelVersion(ModelVersionEntity.builder().draft(true).build())
                        .build(),
                JobEntity.builder()
                        .jobUuid("uuid2")
                        .jobStatus(JobStatus.SUCCESS)
                        .modelVersion(ModelVersionEntity.builder().draft(false).build())
                        .build(),
                JobEntity.builder()
                        .jobUuid("uuid3")
                        .jobStatus(JobStatus.FAIL)
                        .modelVersion(ModelVersionEntity.builder().draft(true).build())
                        .build()
        ));
        when(evaluationRepo.migration(
                "project/1/ftspace/1/eval/summary",
                List.of("uuid2"),
                "project/1/eval/summary")
        ).thenReturn(1);

        var result = fineTuneAppService.exportEvalToCommon(1L, 1L, List.of("uuid1", "uuid2", "uuid3"));
        assertEquals(1, result.getSuccess());
        assertEquals(2, result.getFail());
        verify(evaluationRepo, times(1)).migration(anyString(), eq(List.of("uuid2")), anyString());
    }

    @Test
    void releaseFt() {
        when(fineTuneMapper.findById(1L, 5L)).thenReturn(
                FineTuneEntity.builder()
                        .targetModelVersionId(6L)
                        .spaceId(1L)
                        .build()
        );
        when(modelDao.findByNameForUpdate(any(), anyLong())).thenReturn(ModelEntity.builder().id(124L).build());
        when(modelDao.getModelVersion("6")).thenReturn(ModelVersionEntity
                                                               .builder()
                                                               .modelId(10L)
                                                               .modelName("aac")
                                                               .draft(true)
                                                               .build());
        when(fineTuneSpaceMapper.findById(anyLong())).thenReturn(FineTuneSpaceEntity.builder().projectId(1L).build());
        Assertions.assertThrows(SwValidationException.class, () -> {
            fineTuneAppService.releaseFt(1L, 1L, 5L, null, "aabc", creator);
        });
    }

    @Test
    void releaseAndCreateNew() {
        when(fineTuneMapper.findById(1L, 5L)).thenReturn(
                FineTuneEntity.builder()
                        .targetModelVersionId(6L)
                        .spaceId(1L)
                        .build()
        );
        when(fineTuneSpaceMapper.findById(anyLong())).thenReturn(FineTuneSpaceEntity.builder().projectId(1L).build());
        when(modelDao.getModelVersion("6")).thenReturn(ModelVersionEntity
                                                               .builder()
                                                               .modelId(10L)
                                                               .modelName("aac")
                                                               .draft(true)
                                                               .build());
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ModelEntity) args[0]).setId(123L);
            return null; // void method, so return null
        }).when(modelDao).add(any());
        when(jobMapper.listBizJobs(anyLong(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(List.of(JobEntity.builder().jobUuid("uuid1").build()));

        fineTuneAppService.releaseFt(1L, 1L, 5L, null, "aab", creator);
        verify(modelDao).releaseModelVersion(6L, 123L);
        verify(evaluationRepo, times(1)).updateModelInfo(any(), any(), any(), any());
    }

    @Test
    void releaseSuccessWithBaseModel() {
        when(fineTuneMapper.findById(1L, 5L)).thenReturn(
                FineTuneEntity.builder()
                        .targetModelVersionId(6L)
                        .spaceId(1L)
                        .build()
        );
        when(modelDao.getModelVersion("6")).thenReturn(ModelVersionEntity
                                                               .builder()
                                                               .modelId(10L)
                                                               .modelName("aac")
                                                               .draft(true)
                                                               .build());
        when(modelDao.getModel(anyLong())).thenReturn(ModelEntity.builder().id(10L).build());
        when(jobMapper.listBizJobs(anyLong(), anyString(), anyString(), anyString(), anyLong())).thenReturn(List.of());
        fineTuneAppService.releaseFt(1L, 1L, 5L, 10L, null, creator);
        verify(modelDao).releaseModelVersion(6L, 10L);
        verify(evaluationRepo, times(0)).updateModelInfo(any(), any(), any(), any());
    }

    @Test
    void testTargetVersionReleased() {
        when(fineTuneMapper.findById(1L, 3L)).thenReturn(
                FineTuneEntity.builder()
                        .targetModelVersionId(4L)
                        .build()
        );
        when(modelDao.getModelVersion("4")).thenReturn(ModelVersionEntity
                                                               .builder()
                                                               .draft(false)
                                                               .build());
        Assertions.assertThrows(SwValidationException.class, () -> {
            fineTuneAppService.releaseFt(1L, 1L, 3L, 1L, "", null);
        });
    }

    @Test
    void testTargetVersionNull() {
        when(fineTuneMapper.findById(1L, 2L)).thenReturn(
                FineTuneEntity.builder()
                        .targetModelVersionId(null)
                        .build()
        );
        Assertions.assertThrows(SwNotFoundException.class, () -> {
            fineTuneAppService.releaseFt(1L, 1L, 2L, 1L, "", null);
        });
    }

    @Test
    void testReleaseFtNotFound() {
        when(fineTuneMapper.findById(1L, 1L)).thenReturn(null);
        Assertions.assertThrows(SwNotFoundException.class, () -> {
            fineTuneAppService.releaseFt(1L, 1L, 1L, 1L, "", null);
        });
    }

    @Test
    void testFeatureDisabled() {
        when(featuresProperties.isFineTuneEnabled()).thenReturn(false);
        Assertions.assertThrows(
                StarwhaleApiException.class,
                () -> fineTuneAppService.createFineTune(
                        "1",
                        1L,
                        new JobRequest()
                )
        );

        Assertions.assertThrows(StarwhaleApiException.class, () -> fineTuneAppService.list(1L, 1, 1));
    }
}
