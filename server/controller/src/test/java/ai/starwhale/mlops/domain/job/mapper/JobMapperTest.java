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

package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class JobMapperTest extends MySqlContainerHolder {

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ModelVersionMapper modelVersionMapper;

    UserEntity user;
    ProjectEntity project;
    ModelEntity model;
    ModelVersionEntity modelVersionEntity;
    JobEntity jobPaused;
    JobEntity jobCreated;
    JobEntity jobBuiltIn;

    @BeforeEach
    public void initData() {
        user = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.insert(user);
        project = ProjectEntity.builder().projectName("pjn").ownerId(user.getId()).privacy(1).isDefault(0)
                .build();
        projectMapper.insert(project);
        model = ModelEntity.builder().modelName("model").projectId(project.getId())
                .ownerId(user.getId()).build();
        modelMapper.insert(model);
        modelVersionEntity = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("vn")
                .ownerId(user.getId())
                .jobs("jobs")
                .versionOrder(1L)
                .builtInRuntime("builtin-rt-v1")
                .build();
        modelVersionMapper.insert(modelVersionEntity);
        jobPaused = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(modelVersionEntity.getId())
                .resultOutputPath("").type(JobType.EVALUATION)
                .stepSpec("stepSpec1")
                .projectId(project.getId()).ownerId(user.getId()).build();
        jobCreated = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.CREATED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(modelVersionEntity.getId())
                .resultOutputPath("").type(JobType.EVALUATION)
                .stepSpec("stepSpec2")
                .devMode(true)
                .autoReleaseTime(new Date(123 * 1000L))
                .projectId(project.getId()).ownerId(user.getId()).build();
        jobBuiltIn = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.CREATED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(modelVersionEntity.getId())
                .resultOutputPath("").type(JobType.BUILT_IN)
                .stepSpec("stepSpec2")
                .devMode(true)
                .autoReleaseTime(new Date(123 * 1000L))
                .projectId(project.getId()).ownerId(user.getId()).build();
        jobMapper.addJob(jobPaused);
        jobMapper.addJob(jobCreated);
        jobMapper.addJob(jobBuiltIn);
    }

    @Test
    public void testListJobs() {
        List<JobEntity> jobEntities = jobMapper.listJobs(project.getId(), null);
        Assertions.assertEquals(3, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(getExpectedJob(jobEntity), user,
                        project, modelVersionEntity, jobEntity));
        jobEntities = jobMapper.listJobs(project.getId(), modelVersionEntity.getId());
        Assertions.assertEquals(3, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(getExpectedJob(jobEntity), user,
                        project, modelVersionEntity, jobEntity));
        jobEntities = jobMapper.listJobs(project.getId(), modelVersionEntity.getId() + 1234L);
        Assertions.assertIterableEquals(List.of(), jobEntities);

    }

    private JobEntity getExpectedJob(JobEntity jobEntity) {
        if (jobEntity.getType() == JobType.BUILT_IN) {
            return jobBuiltIn;
        }
        return jobEntity.getJobStatus() == JobStatus.PAUSED ? jobPaused : jobCreated;
    }

    @Test
    public void testListUserJobs() {
        List<JobEntity> jobEntities = jobMapper.listUserJobs(project.getId(), null);
        Assertions.assertEquals(2, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(getExpectedJob(jobEntity), user,
                        project, modelVersionEntity, jobEntity));
        jobEntities = jobMapper.listUserJobs(project.getId(), modelVersionEntity.getId());
        Assertions.assertEquals(2, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(getExpectedJob(jobEntity), user,
                        project, modelVersionEntity, jobEntity));
        jobEntities = jobMapper.listUserJobs(project.getId(), modelVersionEntity.getId() + 1234L);
        Assertions.assertIterableEquals(List.of(), jobEntities);

    }

    @Test
    public void testFindById() {
        validateJob(jobCreated, user, project, modelVersionEntity, jobMapper.findJobById(jobCreated.getId()));
    }

    @Test
    public void testFindByWithoutModelAndRuntime() {
        var virtualJob = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.CREATED)
                .resourcePool("rp").runtimeVersionId(-1L).modelVersionId(-1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .stepSpec("stepSpec2")
                .devMode(true)
                .autoReleaseTime(new Date(123 * 1000L))
                .virtualJobName("vir_n")
                .projectId(project.getId()).ownerId(user.getId()).build();
        jobMapper.addJob(virtualJob);
        var je = jobMapper.findJobById(virtualJob.getId());
        Assertions.assertNull(je.getModelName());
        Assertions.assertTrue(je.getModelVersion().isNull());
        Assertions.assertEquals(-1L, je.getRuntimeVersionId());
        Assertions.assertEquals(-1L, je.getModelVersionId());
        Assertions.assertEquals("vir_n", je.getVirtualJobName());
    }

    @Test
    public void testFindByUuId() {
        validateJob(jobPaused, user, project, modelVersionEntity,
                jobMapper.findJobByUuid(jobPaused.getJobUuid()));
    }

    @Test
    public void testFindByStatusIn() {
        List<JobEntity> jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.PAUSED));
        Assertions.assertEquals(1, jobEntities.size());
        validateJob(jobPaused, user, project, modelVersionEntity, jobEntities.get(0));

        jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.PAUSED, JobStatus.CREATED));
        Assertions.assertEquals(3, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(getExpectedJob(jobEntity), user,
                        project, modelVersionEntity, jobEntity));

        jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.PAUSED, JobStatus.CREATED, JobStatus.FAIL));
        Assertions.assertEquals(3, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(getExpectedJob(jobEntity), user,
                        project, modelVersionEntity, jobEntity));

        jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.CANCELED));
        Assertions.assertEquals(0, jobEntities.size());

    }

    @Test
    public void testUpdateStatus() {
        jobMapper.updateJobStatus(List.of(jobCreated.getId()), JobStatus.SUCCESS);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setJobStatus(JobStatus.SUCCESS);
        validateJob(jobCreated, user, project, modelVersionEntity, jobById);
    }

    @Test
    public void testUpdateFinishedTime() {
        jobMapper.updateJobFinishedTime(List.of(jobCreated.getId()), new Date(), 1000L);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setFinishedTime(new Date());
        jobCreated.setDurationMs(1000L);
        validateJob(jobCreated, user, project, modelVersionEntity, jobById);
    }

    @Test
    public void testUpdateJobComment() {
        String comment = "any comment";
        jobMapper.updateJobComment(jobCreated.getId(), comment);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setComment(comment);
        validateJob(jobCreated, user, project, modelVersionEntity, jobById);
    }

    @Test
    public void testUpdateJobCommentByUuid() {
        String comment = "any comment";
        jobMapper.updateJobCommentByUuid(jobCreated.getJobUuid(), comment);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setComment(comment);
        validateJob(jobCreated, user, project, modelVersionEntity, jobById);
    }

    @Test
    public void testRemoveJob() {
        jobMapper.removeJob(jobCreated.getId());
        jobCreated.setIsDeleted(1);
        validateJob(jobCreated, user, project, modelVersionEntity, jobMapper.findJobById(jobCreated.getId()));

        jobMapper.recoverJob(jobCreated.getId());
        jobCreated.setIsDeleted(0);
        validateJob(jobCreated, user, project, modelVersionEntity, jobMapper.findJobById(jobCreated.getId()));

        jobMapper.removeJobByUuid(jobCreated.getJobUuid());
        jobCreated.setIsDeleted(1);
        validateJob(jobCreated, user, project, modelVersionEntity, jobMapper.findJobById(jobCreated.getId()));

        jobMapper.recoverJobByUuid(jobCreated.getJobUuid());
        jobCreated.setIsDeleted(0);
        validateJob(jobCreated, user, project, modelVersionEntity, jobMapper.findJobById(jobCreated.getId()));
    }

    private void validateJob(JobEntity expectedJob, UserEntity user, ProjectEntity project,
            ModelVersionEntity modelVersionEntity, JobEntity jobEntity) {
        Assertions.assertEquals(expectedJob.getId(), jobEntity.getId());
        Assertions.assertEquals(expectedJob.getJobStatus(), jobEntity.getJobStatus());
        Assertions.assertEquals(expectedJob.getType(), jobEntity.getType());
        Assertions.assertEquals(expectedJob.getResultOutputPath(), jobEntity.getResultOutputPath());
        Assertions.assertEquals(expectedJob.getProjectId(), jobEntity.getProjectId());
        Assertions.assertEquals(expectedJob.getResourcePool(), jobEntity.getResourcePool());
        Assertions.assertEquals(expectedJob.getRuntimeVersionId(), jobEntity.getRuntimeVersionId());
        Assertions.assertEquals(expectedJob.getModelVersionId(), jobEntity.getModelVersionId());
        Assertions.assertEquals(expectedJob.getJobUuid(), jobEntity.getJobUuid());
        Assertions.assertEquals(expectedJob.getDurationMs(), jobEntity.getDurationMs());
        Assertions.assertEquals("model", jobEntity.getModelName());
        Assertions.assertEquals(expectedJob.getStepSpec(), jobEntity.getStepSpec());
        Assertions.assertEquals(expectedJob.getComment(), jobEntity.getComment());
        Assertions.assertEquals(expectedJob.isDevMode(), jobEntity.isDevMode());
        Assertions.assertEquals(expectedJob.getAutoReleaseTime(), jobEntity.getAutoReleaseTime());
        Assertions.assertNotNull(jobEntity.getCreatedTime());
        final int milli500 = 5;
        if (null != expectedJob.getFinishedTime()) {
            Assertions.assertTrue(
                    Math.abs(expectedJob.getFinishedTime().compareTo(jobEntity.getFinishedTime())) <= milli500);
        } else {
            Assertions.assertNull(jobEntity.getFinishedTime());
        }
        Assertions.assertEquals(null == expectedJob.getIsDeleted() ? 0 : expectedJob.getIsDeleted(),
                jobEntity.getIsDeleted());
        validUser(user, jobEntity.getOwner());
        validateModelVersionEntity(modelVersionEntity, user, jobEntity.getModelVersion());
        validProject(project, user, jobEntity.getProject());

    }

    private void validateModelVersionEntity(ModelVersionEntity expected, UserEntity user,
            ModelVersionEntity target) {
        Assertions.assertEquals(expected.getId(), target.getId());
        Assertions.assertEquals(expected.getModelId(), target.getModelId());
        Assertions.assertEquals(expected.getOwnerId(), target.getOwnerId());
        Assertions.assertEquals(expected.getVersionName(), target.getVersionName());
        Assertions.assertEquals(expected.getVersionTag(), target.getVersionTag());
        Assertions.assertNotNull(target.getVersionOrder());
        Assertions.assertEquals("model", target.getModelName());
        Assertions.assertEquals(expected.getName(), target.getName());
        Assertions.assertEquals(expected.getBuiltInRuntime(), target.getBuiltInRuntime());
    }

    private void validProject(ProjectEntity expected, UserEntity user, ProjectEntity target) {
        Assertions.assertEquals(expected.getId(), target.getId());
        Assertions.assertEquals(expected.getOwnerId(), target.getOwnerId());
        Assertions.assertEquals(expected.getProjectName(), target.getProjectName());
        Assertions.assertEquals(expected.getProjectDescription(), target.getProjectDescription());
        Assertions.assertEquals(0, target.getIsDeleted());
        Assertions.assertEquals(0, target.getIsDefault());
        Assertions.assertEquals(expected.getPrivacy(), target.getPrivacy());
    }

    private void validUser(UserEntity expected, UserEntity target) {
        Assertions.assertEquals(expected.getId(), target.getId());
        Assertions.assertEquals(expected.getUserName(), target.getUserName());
        Assertions.assertEquals(expected.getUserEnabled(), target.getUserEnabled());
    }
}
