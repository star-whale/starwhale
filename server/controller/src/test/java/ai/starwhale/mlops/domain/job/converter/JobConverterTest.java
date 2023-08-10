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

package ai.starwhale.mlops.domain.job.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protobuf.Dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protobuf.Dataset.DatasetVo;
import ai.starwhale.mlops.api.protobuf.Job.ExposedLinkVo;
import ai.starwhale.mlops.api.protobuf.Job.ExposedType;
import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.api.protobuf.Model.ModelVersionVo;
import ai.starwhale.mlops.api.protobuf.Model.ModelVo;
import ai.starwhale.mlops.api.protobuf.Model.StepSpec;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeVo;
import ai.starwhale.mlops.api.protobuf.User.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.user.converter.UserVoConverter;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobConverterTest {

    private JobConverter jobConvertor;
    private HotJobHolder hotJobHolder;
    private JobSpecParser jobSpecParser;
    private WebServerInTask webServerInTask;

    @BeforeEach
    public void setUp() {
        RuntimeService runtimeService = mock(RuntimeService.class);
        given(runtimeService.findRuntimeByVersionIds(anyList())).willReturn(List.of(
                RuntimeVo.newBuilder()
                        .setId("1")
                        .setVersion(RuntimeVersionVo.newBuilder()
                                            .setName("rt-v1")
                                            .build())
                        .build()
        ));
        ModelService modelService = mock(ModelService.class);
        given(modelService.findModelByVersionId(anyList())).willReturn(List.of(
                ModelVo.newBuilder()
                        .setId("1")
                        .setVersion(ModelVersionVo.newBuilder()
                                            .setName("model-v1")
                                            .build())
                        .build()
        ));
        DatasetService datasetService = mock(DatasetService.class);
        given(datasetService.findDatasetsByVersionIds(anyList())).willReturn(List.of(
                DatasetVo.newBuilder()
                        .setId("1")
                        .setVersion(DatasetVersionVo.newBuilder()
                                            .setName("ds-v1")
                                            .build())
                        .build()
        ));
        DatasetDao datasetDao = mock(DatasetDao.class);
        IdConverter idConvertor = new IdConverter();
        SystemSettingService systemSettingService = mock(SystemSettingService.class);
        when(systemSettingService.queryResourcePool(anyString())).thenReturn(ResourcePool.defaults());
        hotJobHolder = mock(HotJobHolder.class);
        jobSpecParser = mock(JobSpecParser.class);
        webServerInTask = mock(WebServerInTask.class);
        jobConvertor = new JobConverter(
                idConvertor,
                runtimeService, datasetService, modelService, datasetDao,
                systemSettingService,
                hotJobHolder,
                jobSpecParser,
                1234,
                webServerInTask,
                new UserVoConverter(idConvertor)
        );
    }

    @Test
    public void testConvert() throws JsonProcessingException {
        JobEntity entity = JobEntity.builder()
                .id(1L)
                .jobUuid("job-uuid")
                .owner(UserEntity.builder().id(1L).userName("foo").build())
                .modelName("model")
                .modelVersion(ModelVersionEntity.builder().versionName("v1").builtInRuntime(null).build())
                .modelVersionId(1L)
                .runtimeVersionId(1L)
                .createdTime(new Date(1000L))
                .jobStatus(JobStatus.SUCCESS)
                .finishedTime(new Date(1001L))
                .comment("job-comment")
                .resourcePool("rp")
                .stepSpec("spec")
                .pinnedTime(new Date(1002L))
                .build();
        var stepSpec = StepSpec.newBuilder()
                .setName("step")
                .setJobName("src.evaluator:evaluate")
                .build();
        when(jobSpecParser.parseAndFlattenStepFromYaml(anyString())).thenReturn(List.of(stepSpec));

        var res = jobConvertor.convert(entity);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("uuid", is("job-uuid")),
                hasProperty("owner", isA(UserVo.class)),
                hasProperty("modelName", is("model")),
                hasProperty("modelVersion", is("v1")),
                hasProperty("model", isA(ModelVo.class)),
                hasProperty("jobName", is("src.evaluator:evaluate")),
                hasProperty("createdTime", is(1000L)),
                hasProperty("runtime", isA(RuntimeVo.class)),
                hasProperty("builtinRuntime", is(false)),
                hasProperty("datasetsList", isA(List.class)),
                hasProperty("datasetListList", isA(List.class)),
                hasProperty("jobStatus", is(JobStatus.SUCCESS)),
                hasProperty("stopTime", is(1001L)),
                hasProperty("comment", is("job-comment")),
                hasProperty("resourcePool", is("default")),
                hasProperty("pinnedTime", is(1002L))
        ));
    }

    @Test
    public void testConvertJobExposedLinks() throws JsonProcessingException {
        JobEntity entity = JobEntity.builder()
                .id(1L)
                .jobUuid("job-uuid")
                .owner(UserEntity.builder().id(1L).userName("foo").build())
                .modelName("model")
                .modelVersion(ModelVersionEntity.builder().versionName("v1").build())
                .modelVersionId(1L)
                .runtimeVersionId(1L)
                .jobStatus(JobStatus.SUCCESS)
                .comment("job-comment")
                .resourcePool("rp")
                .build();
        var task = Task.builder()
                .status(TaskStatus.RUNNING)
                .ip("1.1.1.1")
                .id(7L)
                .build();
        var step = Step.builder()
                .name("step")
                .tasks(List.of(task))
                .build();
        var job = Job.builder()
                .id(1L)
                .uuid("job-uuid")
                .status(JobStatus.SUCCESS)
                .stepSpec("step spec")
                .steps(List.of(step))
                .build();

        when(hotJobHolder.ofIds(List.of(1L))).thenReturn(List.of(job));
        var stepSpec = StepSpec.newBuilder()
                .setName("step")
                .setExpose(10)
                .build();
        when(jobSpecParser.parseAndFlattenStepFromYaml(anyString())).thenReturn(List.of(stepSpec));

        // success job won't have exposed links
        var res = jobConvertor.convert(entity);
        assertThat(res.getExposedLinksList().size(), is(0));

        // running job will have exposed links with web handler
        when(webServerInTask.generateGatewayUrl(7L, "1.1.1.1", 10)).thenReturn("/foo/bar/");
        entity.setJobStatus(JobStatus.RUNNING);
        job.setStatus(JobStatus.RUNNING);
        res = jobConvertor.convert(entity);
        var expectWeb = ExposedLinkVo.newBuilder()
                .setType(ExposedType.WEB_HANDLER).setLink("/foo/bar/").setName("step").build();
        assertThat(res.getExposedLinksList(), is(List.of(expectWeb)));

        // get debug mode links when dev mode is on
        when(webServerInTask.generateGatewayUrl(7L, "1.1.1.1", 1234)).thenReturn("/baz/");
        entity.setDevMode(true);
        job.setDevMode(true);
        res = jobConvertor.convert(entity);
        var expectDevMode = ExposedLinkVo.newBuilder()
                .setType(ExposedType.DEV_MODE).setLink("/baz/").setName("VS_CODE").build();
        assertThat(res.getExposedLinksList(), is(List.of(expectDevMode, expectWeb)));
    }
}
