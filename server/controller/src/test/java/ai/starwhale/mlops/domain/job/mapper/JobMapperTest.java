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
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
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
    private SwModelPackageMapper swModelPackageMapper;

    @Autowired
    private SwModelPackageVersionMapper swModelPackageVersionMapper;

    UserEntity user;
    ProjectEntity project;
    SwModelPackageEntity swmp;
    SwModelPackageVersionEntity swModelPackageVersionEntity;
    JobEntity jobPaused;
    JobEntity jobCreated;

    @BeforeEach
    public void initData() {
        user = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.createUser(user);
        project = ProjectEntity.builder().projectName("pjn").ownerId(user.getId()).privacy(1).isDefault(0)
                .build();
        projectMapper.createProject(project);
        swmp = SwModelPackageEntity.builder().swmpName("swmp").projectId(project.getId())
                .ownerId(user.getId()).build();
        swModelPackageMapper.addSwModelPackage(swmp);
        swModelPackageVersionEntity = SwModelPackageVersionEntity.builder()
                .swmpId(swmp.getId())
                .versionName("vn")
                .ownerId(user.getId())
                .evalJobs("stepSpec")
                .manifest("mf")
                .versionMeta("mt")
                .storagePath("s")
                .versionOrder(1L)
                .build();
        swModelPackageVersionMapper.addNewVersion(swModelPackageVersionEntity);
        jobPaused = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePool("rp").runtimeVersionId(1L).swmpVersionId(swModelPackageVersionEntity.getId())
                .resultOutputPath("").type(JobType.EVALUATION)
                .stepSpec("stepSpec1")
                .projectId(project.getId()).ownerId(user.getId()).build();
        jobCreated = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.CREATED)
                .resourcePool("rp").runtimeVersionId(1L).swmpVersionId(swModelPackageVersionEntity.getId())
                .resultOutputPath("").type(JobType.EVALUATION)
                .stepSpec("stepSpec2")
                .projectId(project.getId()).ownerId(user.getId()).build();
        jobMapper.addJob(jobPaused);
        jobMapper.addJob(jobCreated);
    }

    @Test
    public void testListJobs() {
        List<JobEntity> jobEntities = jobMapper.listJobs(project.getId(), null);
        Assertions.assertEquals(2, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(jobEntity.getJobStatus() == JobStatus.PAUSED ? jobPaused : jobCreated, user,
                        project, swModelPackageVersionEntity, jobEntity));
        jobEntities = jobMapper.listJobs(project.getId(), swModelPackageVersionEntity.getId());
        Assertions.assertEquals(2, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(jobEntity.getJobStatus() == JobStatus.PAUSED ? jobPaused : jobCreated, user,
                        project, swModelPackageVersionEntity, jobEntity));
        jobEntities = jobMapper.listJobs(project.getId(), swModelPackageVersionEntity.getId() + 1234L);
        Assertions.assertIterableEquals(List.of(), jobEntities);

    }

    @Test
    public void testFindById() {
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobMapper.findJobById(jobCreated.getId()));
    }

    @Test
    public void testFindByUuId() {
        validateJob(jobPaused, user, project, swModelPackageVersionEntity,
                jobMapper.findJobByUuid(jobPaused.getJobUuid()));
    }

    @Test
    public void testFindByStatusIn() {
        List<JobEntity> jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.PAUSED));
        Assertions.assertEquals(1, jobEntities.size());
        validateJob(jobPaused, user, project, swModelPackageVersionEntity, jobEntities.get(0));

        jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.PAUSED, JobStatus.CREATED));
        Assertions.assertEquals(2, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(jobEntity.getJobStatus() == JobStatus.PAUSED ? jobPaused : jobCreated, user,
                        project, swModelPackageVersionEntity, jobEntity));

        jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.PAUSED, JobStatus.CREATED, JobStatus.FAIL));
        Assertions.assertEquals(2, jobEntities.size());
        jobEntities.forEach(
                jobEntity -> validateJob(jobEntity.getJobStatus() == JobStatus.PAUSED ? jobPaused : jobCreated, user,
                        project, swModelPackageVersionEntity, jobEntity));

        jobEntities = jobMapper.findJobByStatusIn(List.of(JobStatus.CANCELED));
        Assertions.assertEquals(0, jobEntities.size());

    }

    @Test
    public void testUpdateStatus() {
        jobMapper.updateJobStatus(List.of(jobCreated.getId()), JobStatus.SUCCESS);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setJobStatus(JobStatus.SUCCESS);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobById);
    }

    @Test
    public void testUpdateFinishedTime() {
        jobMapper.updateJobFinishedTime(List.of(jobCreated.getId()), new Date());
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setFinishedTime(new Date());
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobById);
    }

    @Test
    public void testeUpdateJobComment() {
        String comment = "any comment";
        jobMapper.updateJobComment(jobCreated.getId(), comment);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setComment(comment);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobById);
    }

    @Test
    public void testUpdateJobCommentByUuid() {
        String comment = "any comment";
        jobMapper.updateJobCommentByUuid(jobCreated.getJobUuid(), comment);
        JobEntity jobById = jobMapper.findJobById(jobCreated.getId());
        jobCreated.setComment(comment);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobById);
    }

    @Test
    public void testRemoveJob() {
        jobMapper.removeJob(jobCreated.getId());
        jobCreated.setIsDeleted(1);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobMapper.findJobById(jobCreated.getId()));

        jobMapper.recoverJob(jobCreated.getId());
        jobCreated.setIsDeleted(0);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobMapper.findJobById(jobCreated.getId()));

        jobMapper.removeJobByUuid(jobCreated.getJobUuid());
        jobCreated.setIsDeleted(1);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobMapper.findJobById(jobCreated.getId()));

        jobMapper.recoverJobByUuid(jobCreated.getJobUuid());
        jobCreated.setIsDeleted(0);
        validateJob(jobCreated, user, project, swModelPackageVersionEntity, jobMapper.findJobById(jobCreated.getId()));
    }

    private void validateJob(JobEntity expectedJob, UserEntity user, ProjectEntity project,
            SwModelPackageVersionEntity swModelPackageVersionEntity, JobEntity jobEntity) {
        Assertions.assertEquals(expectedJob.getId(), jobEntity.getId());
        Assertions.assertEquals(expectedJob.getJobStatus(), jobEntity.getJobStatus());
        Assertions.assertEquals(expectedJob.getType(), jobEntity.getType());
        Assertions.assertEquals(expectedJob.getResultOutputPath(), jobEntity.getResultOutputPath());
        Assertions.assertEquals(expectedJob.getProjectId(), jobEntity.getProjectId());
        Assertions.assertEquals(expectedJob.getResourcePool(), jobEntity.getResourcePool());
        Assertions.assertEquals(expectedJob.getRuntimeVersionId(), jobEntity.getRuntimeVersionId());
        Assertions.assertEquals(expectedJob.getSwmpVersionId(), jobEntity.getSwmpVersionId());
        Assertions.assertEquals(expectedJob.getJobUuid(), jobEntity.getJobUuid());
        Assertions.assertEquals(expectedJob.getDurationMs(), jobEntity.getDurationMs());
        Assertions.assertEquals("swmp", jobEntity.getModelName());
        Assertions.assertEquals(expectedJob.getStepSpec(), jobEntity.getStepSpec());
        Assertions.assertEquals(expectedJob.getComment(), jobEntity.getComment());
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
        validateSwModelPackageVersionEntity(swModelPackageVersionEntity, user, jobEntity.getSwmpVersion());
        validProject(project, user, jobEntity.getProject());

    }

    private void validateSwModelPackageVersionEntity(SwModelPackageVersionEntity expected, UserEntity user,
            SwModelPackageVersionEntity target) {
        Assertions.assertEquals(expected.getId(), target.getId());
        Assertions.assertEquals(expected.getSwmpId(), target.getSwmpId());
        Assertions.assertEquals(expected.getOwnerId(), target.getOwnerId());
        Assertions.assertEquals(expected.getVersionName(), target.getVersionName());
        Assertions.assertEquals(expected.getStoragePath(), target.getStoragePath());
        Assertions.assertEquals(expected.getVersionTag(), target.getVersionTag());
        Assertions.assertEquals(expected.getManifest(), target.getManifest());
        Assertions.assertEquals(expected.getEvalJobs(), target.getEvalJobs());
        Assertions.assertNotNull(target.getVersionOrder());
        Assertions.assertEquals("swmp", target.getSwmpName());
        Assertions.assertEquals(expected.getName(), target.getName());
        validUser(user, target.getOwner());
    }

    private void validProject(ProjectEntity expected, UserEntity user, ProjectEntity target) {
        Assertions.assertEquals(expected.getId(), target.getId());
        Assertions.assertEquals(expected.getOwnerId(), target.getOwnerId());
        Assertions.assertEquals(expected.getProjectName(), target.getProjectName());
        Assertions.assertEquals(expected.getDescription(), target.getDescription());
        Assertions.assertEquals(0, target.getIsDeleted());
        Assertions.assertEquals(0, target.getIsDefault());
        Assertions.assertEquals(expected.getPrivacy(), target.getPrivacy());
        validUser(user, target.getOwner());
    }

    private void validUser(UserEntity expected, UserEntity target) {
        Assertions.assertEquals(expected.getId(), target.getId());
        Assertions.assertEquals(expected.getUserName(), target.getUserName());
        Assertions.assertEquals(expected.getUserEnabled(), target.getUserEnabled());
    }
}
