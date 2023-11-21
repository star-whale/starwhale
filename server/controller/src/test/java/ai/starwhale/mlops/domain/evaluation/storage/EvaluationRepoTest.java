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

package ai.starwhale.mlops.domain.evaluation.storage;

import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.INT64;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.KeyColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelNameColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelUriColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelUriViewColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelVersionColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelVersionIdColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.STRING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EvaluationRepoTest {

    private EvaluationRepo evaluationRepo;

    private DataStore dataStore;

    private ProjectService projectService;

    private ModelService modelService;

    private UserService userService;

    @BeforeEach
    public void initData() {
        this.projectService = mock(ProjectService.class);
        this.modelService = mock(ModelService.class);
        this.userService = mock(UserService.class);
        this.dataStore = mock(DataStore.class);
        evaluationRepo = new EvaluationRepo(dataStore, projectService, modelService, userService, new ObjectMapper());
    }

    @Test
    public void testAddJob() {
        Mockito.when(projectService.findProject(1L))
                .thenReturn(Project.builder().id(1L).name("test-project").build());

        JobFlattenEntity jobEntity = JobFlattenEntity.builder()
                .id(1L)
                .name("mnist:eval")
                .jobUuid("1q2w3e4r5t6y")
                .ownerId(1L)
                .runtimeUriForView("p/p-1/runtime/rt/version/1a2s3d4f5g6h")
                .runtimeVersionId(3L)
                .runtimeVersionValue("1a2s3d4f5g6h")
                .runtimeName("test-rt")
                .projectId(1L)
                .project(Project.builder().id(1L).name("test-project").build())
                .modelUri("p/p-1/model/m/version/1z2x3c4v5b6n")
                .modelVersionId(3L)
                .modelVersionValue("1z2x3c4v5b6n")
                .modelName("test-model")
                .datasetIdVersionMap(Map.of(1L, "qwerty", 2L, "asdfgh"))
                .datasets(List.of("p/p-1/dataset/ds/version/1q2w3e4r"))
                .comment("")
                .resultOutputPath("path/result/test")
                .jobStatus(JobStatus.CREATED)
                .type(JobType.EVALUATION)
                .resourcePool(ResourcePool.DEFAULT_NAME)
                .stepSpec("step spec")
                .createdTime(new Date())
                .modifiedTime(new Date())
                .build();
        evaluationRepo.addJob("project/1/eval/summary", jobEntity);

        verify(dataStore, times(1))
                .update(eq("project/1/eval/summary"), any(), anyList());

        assertThat("convert",
                   evaluationRepo.convertToDatastoreValue(jobEntity.getDatasetIdVersionMap()),
                   is(Map.of("0000000000000001", "qwerty", "0000000000000002", "asdfgh"))
        );

    }

    @Test
    public void testUpdateProperty() {
        var jobId = 123456L;
        var uuid = "1q2w3e4r5t6y";
        var table = "project/1/eval/summary";
        Mockito.when(projectService.listProjects())
                .thenReturn(List.of(Project.builder().id(1L).name("test-project").build()));
        var job = Job.builder()
                .id(jobId)
                .uuid(uuid)
                .type(JobType.EVALUATION)
                .project(Project.builder().id(1L).name("test-project").build())
                .build();

        evaluationRepo.updateJobStatus(table, job, JobStatus.SUCCESS);
        verify(dataStore, times(1)).update(eq(table), any(), anyList());

        evaluationRepo.updateJobFinishedTime(table, job, Date.from(Instant.now()), 100L);
        verify(dataStore, times(2))
                .update(eq(table), any(), anyList());

        evaluationRepo.removeJob(table, job);
        verify(dataStore, times(3))
                .update(eq(table), any(), anyList());

        evaluationRepo.recoverJob(table, job);
        verify(dataStore, times(4))
                .update(eq(table), any(), anyList());

        evaluationRepo.updateJobComment(table, job, "test1");
        verify(dataStore, times(5))
                .update(eq(table), any(), anyList());

        reset(dataStore);
        job.setType(JobType.BUILT_IN);
        evaluationRepo.updateJobComment(table, job, "any");
        evaluationRepo.recoverJob(table, job);
        evaluationRepo.updateJobFinishedTime(table, job, new Date(), 100L);
        evaluationRepo.updateJobStatus(table, job, JobStatus.RUNNING);
        verify(dataStore, times(0))
                .update(eq(table), any(), anyList());
    }

    @Test
    public void testUpdateModelInfo() {
        var table = "p/1/space/1/eval/summary";
        var model = ModelEntity.builder().id(1L).projectId(1L).modelName("m-name").build();
        var version = ModelVersionEntity.builder().id(11L).versionName("v-name").build();

        var res = evaluationRepo.updateModelInfo(null, List.of("uuid1", "uuid2"), model, version);
        assertEquals(0, res);
        res = evaluationRepo.updateModelInfo(table, List.of(), model, version);
        assertEquals(0, res);
        res = evaluationRepo.updateModelInfo(table, List.of("uuid1", "uuid2"), null, version);
        assertEquals(0, res);
        res = evaluationRepo.updateModelInfo(table, List.of("uuid1", "uuid2"), model, null);
        assertEquals(0, res);

        given(projectService.findProject(1L)).willReturn(Project.builder().id(1L).name("p-name").build());
        res = evaluationRepo.updateModelInfo(table, List.of("uuid1", "uuid2"), model, version);
        assertEquals(2, res);
        verify(dataStore, times(1)).update(
                table,
                new TableSchemaDesc(KeyColumn, List.of(
                        ColumnSchemaDesc.builder().name(KeyColumn).type(STRING).build(),
                        ColumnSchemaDesc.builder().name(ModelNameColumn).type(STRING).build(),
                        ColumnSchemaDesc.builder().name(ModelUriColumn).type(STRING).build(),
                        ColumnSchemaDesc.builder().name(ModelUriViewColumn).type(STRING).build(),
                        ColumnSchemaDesc.builder().name(ModelVersionColumn).type(STRING).build(),
                        ColumnSchemaDesc.builder().name(ModelVersionIdColumn).type(INT64).build()

                )),
                List.of(
                        Map.of(
                                KeyColumn, "uuid1",
                                ModelNameColumn, "m-name",
                                ModelVersionColumn, "v-name",
                                ModelUriViewColumn, "project/p-name/model/m-name/version/v-name",
                                ModelUriColumn, "project/1/model/1/version/11",
                                ModelVersionIdColumn, "000000000000000b"
                        ),
                        Map.of(
                                KeyColumn, "uuid2",
                                ModelNameColumn, "m-name",
                                ModelVersionColumn, "v-name",
                                ModelUriViewColumn, "project/p-name/model/m-name/version/v-name",
                                ModelUriColumn, "project/1/model/1/version/11",
                                ModelVersionIdColumn, "000000000000000b"
                        )
                )
        );
    }

}
