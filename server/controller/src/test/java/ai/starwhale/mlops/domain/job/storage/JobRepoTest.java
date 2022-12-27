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

package ai.starwhale.mlops.domain.job.storage;

import static ai.starwhale.mlops.domain.job.JobSchema.JobStatusColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.KeyColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.LongIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelVersionColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelVersionIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ProjectIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeVersionIdColumn;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.RecordList;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.exception.SwProcessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class JobRepoTest {

    private JobRepo jobRepo;

    private DataStore dataStore;

    private ProjectMapper projectMapper;

    private ModelVersionMapper modelVersionMapper;

    private UserMapper userMapper;

    @BeforeEach
    public void initData() {
        this.projectMapper = mock(ProjectMapper.class);
        this.modelVersionMapper = mock(ModelVersionMapper.class);
        this.userMapper = mock(UserMapper.class);
        this.dataStore = mock(DataStore.class);
        jobRepo = new JobRepo(dataStore, projectMapper, modelVersionMapper, userMapper, new ObjectMapper());
    }

    @Test
    public void testAddJob() {
        Mockito.when(projectMapper.find(1L))
                .thenReturn(ProjectEntity.builder().projectName("test-project").build());

        JobEntity jobEntity = JobEntity.builder()
                .id(1L)
                .jobUuid("1q2w3e4r5t6y")
                .ownerId(1L)
                .runtimeVersionId(1L)
                .runtimeVersionValue("1a2s3d4f5g6h")
                .runtimeName("test-rt")
                .projectId(1L)
                .modelVersionId(1L)
                .modelVersionValue("1z2x3c4v5b6n")
                .modelName("test-model")
                .datasetIdVersionMap(Map.of(1L, "qwerty", 2L, "asdfgh"))
                .comment("")
                .resultOutputPath("path/result/test")
                .jobStatus(JobStatus.CREATED)
                .type(JobType.EVALUATION)
                .resourcePool(ResourcePool.DEFAULT_NAME)
                .stepSpec("step spec")
                .createdTime(new Date())
                .modifiedTime(new Date())
                .build();
        jobRepo.addJob(jobEntity);

        verify(dataStore, times(1))
                .update(eq("project/test-project/eval/summary"), any(), anyList());

        assertThat("convert",
                jobRepo.convert(jobEntity.getDatasetIdVersionMap()),
                is(Map.of("0000000000000001", "qwerty", "0000000000000002", "asdfgh"))
        );
    }

    @Test
    public void testListJobs() {
        Mockito.when(projectMapper.find(1L))
                .thenReturn(ProjectEntity.builder().projectName("test-project").build());
        Mockito.when(modelVersionMapper.findByNameAndModelId(any(), any()))
                .thenReturn(ModelVersionEntity.builder().id(1L).versionName("1z2x3c4v5b6n").build());

        Mockito.when(dataStore.query(any()))
                .thenReturn(new RecordList(Map.of(), List.of(
                    Map.of(
                        KeyColumn, "1q2w3e4r5t6y",
                        LongIdColumn, "1",
                        ProjectIdColumn, "0000000000000001",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000001",
                        JobStatusColumn, "RUNNING"
                    ),
                    Map.of(
                        KeyColumn, "1a2s3d4f5g6h",
                        LongIdColumn, "2",
                        ProjectIdColumn, "0000000000000002",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000002",
                        JobStatusColumn, "RUNNING"
                    )
                ), null))
                .thenReturn(new RecordList(Map.of(), List.of(), null));

        List<JobEntity> jobEntities = jobRepo.listJobs(1L, null);
        Assertions.assertEquals(2, jobEntities.size());
        Mockito.verify(dataStore, times(2)).query(any());
        jobEntities.forEach(job -> Assertions.assertEquals(job.getJobStatus(), JobStatus.RUNNING));
    }

    @Test
    public void testFindById() {
        Mockito.when(projectMapper.list(null, null, null))
                .thenReturn(List.of(ProjectEntity.builder().projectName("test-project").build()));
        // mock find multi rows, so there would have an exception
        Mockito.when(dataStore.query(any()))
                .thenReturn(new RecordList(Map.of(), List.of(
                    Map.of(
                        KeyColumn, "1q2w3e4r5t6y",
                        LongIdColumn, "1",
                        ProjectIdColumn, "0000000000000001",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000001",
                        JobStatusColumn, "RUNNING"
                    ),
                    Map.of(
                        KeyColumn, "1a2s3d4f5g6h",
                        LongIdColumn, "2",
                        ProjectIdColumn, "0000000000000002",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000002",
                        JobStatusColumn, "RUNNING"
                    )
                ), null));
        Assertions.assertThrows(SwProcessException.class, () -> jobRepo.findJobById(123456L));

        // normal test
        Mockito.when(dataStore.query(any()))
                .thenReturn(new RecordList(Map.of(), List.of(
                    Map.of(
                        KeyColumn, "1q2w3e4r5t6y",
                        LongIdColumn, "1",
                        ProjectIdColumn, "0000000000000001",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000001",
                        JobStatusColumn, "SUCCESS"
                    )
                ), null));
        var job = jobRepo.findJobById(123456L);
        Assertions.assertNotNull(job);
        verify(modelVersionMapper, times(1)).findByNameAndModelId(any(), any());
        Assertions.assertEquals(job.getJobStatus(), JobStatus.SUCCESS);
    }

    @Test
    public void testFindByStatusIn() {
        Mockito.when(projectMapper.list(null, null, null))
                .thenReturn(List.of(ProjectEntity.builder().projectName("test-project").build()));
        Mockito.when(modelVersionMapper.findByNameAndModelId(any(), any()))
                .thenReturn(ModelVersionEntity.builder().id(1L).versionName("1z2x3c4v5b6n").build());

        Mockito.when(dataStore.query(any()))
                .thenReturn(new RecordList(Map.of(), List.of(
                    Map.of(
                        KeyColumn, "1q2w3e4r5t6y",
                        LongIdColumn, "1",
                        ProjectIdColumn, "0000000000000001",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000001",
                        JobStatusColumn, "RUNNING"
                    ),
                    Map.of(
                        KeyColumn, "1a2s3d4f5g6h",
                        LongIdColumn, "2",
                        ProjectIdColumn, "0000000000000002",
                        ModelVersionIdColumn, "0000000000000001",
                        ModelVersionColumn, "1z2x3c4v5b6n",
                        RuntimeVersionIdColumn, "0000000000000002",
                        JobStatusColumn, "RUNNING"
                    )
                ), null))
                .thenReturn(new RecordList(Map.of(), List.of(), null));;

        List<JobEntity> jobEntities = jobRepo.findJobByStatusIn(List.of(JobStatus.PAUSED));
        Assertions.assertEquals(2, jobEntities.size());
        Mockito.verify(dataStore, times(2)).query(any());
        jobEntities.forEach(job -> Assertions.assertEquals(job.getJobStatus(), JobStatus.RUNNING));
    }

    @Test
    public void testUpdateStatus() {
        Mockito.when(projectMapper.list(null, null, null))
                .thenReturn(List.of(ProjectEntity.builder().id(1L).projectName("test-project").build()));
        Mockito.when(modelVersionMapper.findByNameAndModelId(any(), any()))
                .thenReturn(ModelVersionEntity.builder().id(1L).versionName("1z2x3c4v5b6n").build());
        Mockito.when(dataStore.query(any()))
                .thenReturn(new RecordList(Map.of(), List.of(Map.of(KeyColumn, "1q2w3e4r5t6y")), null));

        jobRepo.updateJobStatus(123456L, JobStatus.SUCCESS);
        verify(dataStore, times(1)).update(eq("project/test-project/eval/summary"), any(), anyList());
    }
}
