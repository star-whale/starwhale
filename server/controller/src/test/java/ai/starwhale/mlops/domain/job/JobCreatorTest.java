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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobCreateRequest;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.UserJobConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobCreatorTest {

    private JobSpliterator jobSpliterator;
    private JobLoader jobLoader;
    private StoragePathCoordinator storagePathCoordinator;
    private JobDao jobDao;
    private JobUpdateHelper jobUpdateHelper;

    private SystemSettingService systemSettingService;
    private JobSpecParser jobSpecParser;

    private JobCreator jobCreator;
    private UserJobConverter userJobConverter;

    @BeforeEach
    public void setup() {
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
        systemSettingService = mock(SystemSettingService.class);
        jobUpdateHelper = mock(JobUpdateHelper.class);
        userJobConverter = mock(UserJobConverter.class);
        jobSpecParser = new JobSpecParser();
        jobCreator = new JobCreator(
                jobSpliterator,
                jobLoader,
                storagePathCoordinator,
                jobDao,
                jobUpdateHelper,
                systemSettingService,
                jobSpecParser,
                userJobConverter
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
        given(storagePathCoordinator.allocateResultMetricsPath("uuid1"))
                .willReturn("out");
        given(jobDao.addJob(any(JobFlattenEntity.class)))
                .willAnswer(invocation -> {
                    JobFlattenEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return true;
                });

        var jobReq = JobCreateRequest.builder()
                .handler("mnist.evaluator:MNISTInference.cmp")
                .stepSpecOverWrites(overviewJobSpec)
                .jobType(JobType.EVALUATION)
                .build();

        // handler and stepSpec could only have one
        assertThrows(StarwhaleApiException.class, () -> jobCreator.createJob(jobReq, entity -> {}));

        jobReq.setHandler(null);
        jobReq.setStepSpecOverWrites(null);
        assertThrows(StarwhaleApiException.class, () -> jobCreator.createJob(jobReq, entity -> {}));


        var userJobReq = UserJobCreateRequest.builder()
                .project(Project.builder().id(1L).build())
                .modelVersionId(3L)
                .datasetVersionIds(List.of(1L))
                .runtimeVersionId(2L)
                .handler("mnist.evaluator:MNISTInference.cmp")
                .resourcePool("1")
                .comment("")
                .jobType(JobType.EVALUATION)
                .devMode(false)
                .devWay(DevWay.VS_CODE)
                .devPassword("")
                .ttlInSec(1L)
                .user(User.builder().id(1L).build())
                .build();

        var flatten = JobFlattenEntity.builder()
                .jobUuid("uuid1")
                .project(Project.builder().id(1L).build())
                .modelVersionId(3L)
                .modelVersion(ModelVersion.builder().id(3L).jobs(fullJobSpec).build())
                .runtimeVersionId(2L)
                .resourcePool("1")
                .comment("")
                .devMode(false)
                .devWay(null)
                .devPassword(null);

        given(userJobConverter.convert(userJobReq)).willReturn(flatten);

        var res = jobCreator.createJob(userJobReq, entity -> {});
        assertThat(res, is(Job.builder().id(1L).type(JobType.EVALUATION).build()));
        verify(jobDao).addJob(argThat(jobFlattenEntity -> !jobFlattenEntity.isDevMode()
                && jobFlattenEntity.getDevWay() == null && jobFlattenEntity.getDevPassword() == null));

        flatten.devMode(true).devWay(DevWay.VS_CODE).devPassword("");
        res = jobCreator.createJob(userJobReq, entity -> {});
        assertThat(res, is(Job.builder().id(1L).type(JobType.EVALUATION).build()));
        verify(jobDao).addJob(argThat(jobFlattenEntity -> jobFlattenEntity.isDevMode()
                && jobFlattenEntity.getDevWay() == DevWay.VS_CODE && jobFlattenEntity.getDevPassword().isEmpty()));

    }
}
