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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.bo.Runtime;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobCreatorTest {

    private JobBoConverter jobBoConverter;
    private JobSpliterator jobSpliterator;
    private JobLoader jobLoader;
    private StoragePathCoordinator storagePathCoordinator;
    private JobDao jobDao;
    private ModelService modelService;
    private DatasetDao datasetDao;
    private RuntimeService runtimeService;
    private JobUpdateHelper jobUpdateHelper;

    private SystemSettingService systemSettingService;
    private JobSpecParser jobSpecParser;

    private JobCreator jobCreator;

    @BeforeEach
    public void setup() {
        jobBoConverter = mock(JobBoConverter.class);
        jobSpliterator = mock(JobSpliterator.class);
        jobLoader = mock(JobLoader.class);
        storagePathCoordinator = mock(StoragePathCoordinator.class);
        jobDao = mock(JobDao.class);
        given(jobDao.findJob("1"))
                .willReturn(Job.builder().id(1L).type(JobType.EVALUATION).build());
        given(jobDao.findJobById(1L))
                .willReturn(Job.builder().id(1L).type(JobType.EVALUATION).build());
        given(jobDao.findJobEntity("1"))
                .willReturn(JobEntity.builder().id(1L).type(JobType.EVALUATION).build());
        given(jobDao.getJobId("1"))
                .willReturn(1L);
        given(jobDao.getJobId("2"))
                .willReturn(2L);
        modelService = mock(ModelService.class);
        runtimeService = mock(RuntimeService.class);
        systemSettingService = mock(SystemSettingService.class);
        datasetDao = mock(DatasetDao.class);
        jobUpdateHelper = mock(JobUpdateHelper.class);
        jobSpecParser = new JobSpecParser();
        jobCreator = new JobCreator(
                jobBoConverter,
                jobSpliterator,
                jobLoader,
                storagePathCoordinator,
                jobDao,
                modelService,
                datasetDao,
                runtimeService,
                jobUpdateHelper,
                systemSettingService,
                jobSpecParser
        );
    }

    @Test
    public void testCreateJob() {
        String fullJobSpec = "mnist.evaluator:MNISTInference.cmp:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs: []\n"
                + "  resources: []\n"
                + "  name: mnist.evaluator:MNISTInference.ppl\n"
                + "  replicas: 1\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs:\n"
                + "  - mnist.evaluator:MNISTInference.ppl\n"
                + "  resources:\n"
                + "  - type: cpu \n"
                + "    request: 0.1\n"
                + "    limit: 0.1\n"
                + "  - type: nvidia.com/gpu \n"
                + "    request: 1\n"
                + "    limit: 1\n"
                + "  - type: memory \n"
                + "    request: 1\n"
                + "    limit: 1\n"
                + "  name: mnist.evaluator:MNISTInference.cmp\n"
                + "  replicas: 1\n"
                + "mnist.evaluator:MNISTInference.ppl:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs: []\n"
                + "  resources: []\n"
                + "  name: mnist.evaluator:MNISTInference.ppl\n"
                + "  replicas: 1";
        String overviewJobSpec = "mnist.evaluator:MNISTInference.cmp:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs: []\n"
                + "  resources: []\n"
                + "  name: mnist.evaluator:MNISTInference.ppl\n"
                + "  replicas: 1\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs:\n"
                + "  - mnist.evaluator:MNISTInference.ppl\n"
                + "  resources:\n"
                + "  - type: cpu \n"
                + "    request: 0.1\n"
                + "    limit: 0.1\n"
                + "  - type: nvidia.com/gpu \n"
                + "    request: 1\n"
                + "    limit: 1\n"
                + "  - type: memory \n"
                + "    request: 1\n"
                + "    limit: 1\n"
                + "  name: mnist.evaluator:MNISTInference.cmp\n"
                + "  parameters_sig:\n"
                + "    - name: a\n"
                + "      required: 'false'\n"
                + "    - name: b\n"
                + "      required: 'false'\n"
                + "    - name: c\n"
                + "      required: 'false'\n"
                + "  ext_cmd_args: '--a 1'\n"
                + "  replicas: 1";
        given(runtimeService.findRuntimeVersion(same("2")))
                .willReturn(RuntimeVersion.builder().id(2L).runtimeId(2L).versionName("1r2t3y4u5i6").build());
        given(runtimeService.findRuntime(same(2L)))
                .willReturn(Runtime.builder().id(2L).name("test-runtime").build());
        given(modelService.findModelVersion(same("3")))
                .willReturn(ModelVersion.builder().id(3L).modelId(3L).name("q1w2e3r4t5y6").jobs(fullJobSpec).build());
        given(modelService.findModel(same(3L)))
                .willReturn(Model.builder().id(3L).projectId(10L).name("test-model").build());
        given(storagePathCoordinator.allocateResultMetricsPath("uuid1"))
                .willReturn("out");
        given(jobDao.addJob(any(JobFlattenEntity.class)))
                .willAnswer(invocation -> {
                    JobFlattenEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return true;
                });
        given(datasetDao.getDatasetVersion(anyString()))
                .willReturn(DatasetVersion.builder().id(1L).versionName("a1s2d3f4g5h6").build());

        assertThrows(StarwhaleApiException.class,
                () -> jobCreator.createJob(Project.builder().name("1").build(), "3", "1", "2",
                        "", "1", "", "", JobType.EVALUATION, DevWay.VS_CODE, false, "", 1L,
                        User.builder().id(1L).build()));

        assertThrows(StarwhaleApiException.class,
                () -> jobCreator.createJob(Project.builder().name("1").build(), "3", "1", "2",
                        "", "1", "h", "s", JobType.EVALUATION, DevWay.VS_CODE, false, "", 1L,
                        User.builder().id(1L).build()));

        // use built-in runtime(but no built-in)
        assertThrows(SwValidationException.class,
                () -> jobCreator.createJob(Project.builder().name("1").build(), "3", "1", "",
                        "", "1", "h", "s", JobType.EVALUATION, DevWay.VS_CODE, false, "", 1L,
                        User.builder().id(1L).build()));

        var res = jobCreator.createJob(Project.builder().name("1").build(), "3", "1", "2",
                "", "1", "mnist.evaluator:MNISTInference.cmp", "", JobType.EVALUATION, DevWay.VS_CODE, false, "", 1L,
                User.builder().id(1L).build());
        assertThat(res, is(Job.builder().id(1L).type(JobType.EVALUATION).build()));
        verify(jobDao).addJob(argThat(jobFlattenEntity -> !jobFlattenEntity.isDevMode()
                && jobFlattenEntity.getDevWay() == null && jobFlattenEntity.getDevPassword() == null));

        res = jobCreator.createJob(Project.builder().name("1").build(), "3", "1", "2",
                "", "1", "", overviewJobSpec, JobType.FINE_TUNE, DevWay.VS_CODE, true, "", 1L,
                User.builder().id(1L).build());
        assertThat(res, is(Job.builder().id(1L).type(JobType.EVALUATION).build()));
        verify(jobDao).addJob(argThat(jobFlattenEntity -> jobFlattenEntity.isDevMode()
                && jobFlattenEntity.getDevWay() == DevWay.VS_CODE && jobFlattenEntity.getDevPassword().equals("")));

        // use built-in runtime(but no built-in)
        var builtInRuntime = "built-in-rt";
        given(modelService.findModelVersion(same("3"))).willReturn(
                ModelVersion.builder()
                        .id(3L)
                        .modelId(3L)
                        .name("q1w2e3r4t5y6")
                        .builtInRuntime(builtInRuntime)
                        .jobs(fullJobSpec)
                        .build()
        );
        given(runtimeService.findBuiltInRuntimeVersion(10L, builtInRuntime))
                .willReturn(RuntimeVersion.builder().id(2L).runtimeId(2L).build());
        res = jobCreator.createJob(Project.builder().name("1").build(), "3", "1", "",
                "", "1", "", overviewJobSpec, JobType.FINE_TUNE, DevWay.VS_CODE, true, "", 1L,
                User.builder().id(1L).build());
        assertThat(res, is(Job.builder().id(1L).type(JobType.EVALUATION).build()));
        verify(runtimeService).findBuiltInRuntimeVersion(eq(10L), eq(builtInRuntime));
    }

}
