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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.context.ApplicationContext;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class TestJobMapper extends MySqlContainerHolder {

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

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testListJobs() {
        UserEntity user = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.createUser(user);
        ProjectEntity project = ProjectEntity.builder().projectName("pjn").ownerId(user.getId()).privacy(1).isDefault(0)
                .build();
        projectMapper.createProject(project);
        SwModelPackageEntity swmp = SwModelPackageEntity.builder().swmpName("swmp").projectId(project.getId())
                .ownerId(user.getId()).build();
        swModelPackageMapper.addSwModelPackage(swmp);
        SwModelPackageVersionEntity swModelPackageVersionEntity = SwModelPackageVersionEntity.builder()
                .swmpId(swmp.getId())
                .versionName("vn")
                .ownerId(user.getId()).evalJobs("")
                .manifest("mf").versionMeta("mt").storagePath("s").build();
        swModelPackageVersionMapper.addNewVersion(swModelPackageVersionEntity);
        JobEntity job = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePoolId(1L).runtimeVersionId(1L).swmpVersionId(swModelPackageVersionEntity.getId())
                .resultOutputPath("").type(JobType.EVALUATION)
                .deviceType(0).deviceAmount(1).projectId(project.getId()).ownerId(user.getId()).build();
        jobMapper.addJob(job);
        List<JobEntity> jobEntities = jobMapper.listJobs(project.getId(), null);
        Assertions.assertEquals(1, jobEntities.size());
        validateJob(job, user, project, swModelPackageVersionEntity, jobEntities.get(0));
        jobEntities = jobMapper.listJobs(project.getId(), swModelPackageVersionEntity.getId());
        Assertions.assertEquals(1, jobEntities.size());
        validateJob(job, user, project, swModelPackageVersionEntity, jobEntities.get(0));
        jobEntities = jobMapper.listJobs(project.getId(), swModelPackageVersionEntity.getId() + 1234L);
        Assertions.assertIterableEquals(List.of(), jobEntities);

    }

    private void validateJob(JobEntity expectedJob, UserEntity user, ProjectEntity project,
            SwModelPackageVersionEntity swModelPackageVersionEntity, JobEntity jobEntity) {
        Assertions.assertEquals(expectedJob.getId(), jobEntity.getId());
        Assertions.assertEquals(expectedJob.getDeviceType(), jobEntity.getDeviceType());
        Assertions.assertEquals(expectedJob.getJobStatus(), jobEntity.getJobStatus());
        Assertions.assertEquals(expectedJob.getDeviceAmount(), jobEntity.getDeviceAmount());
        Assertions.assertEquals(expectedJob.getType(), jobEntity.getType());
        Assertions.assertEquals(expectedJob.getResultOutputPath(), jobEntity.getResultOutputPath());
        Assertions.assertEquals(expectedJob.getProjectId(), jobEntity.getProjectId());
        Assertions.assertEquals(expectedJob.getResourcePoolId(), jobEntity.getResourcePoolId());
        Assertions.assertEquals(expectedJob.getRuntimeVersionId(), jobEntity.getRuntimeVersionId());
        Assertions.assertEquals(expectedJob.getSwmpVersionId(), jobEntity.getSwmpVersionId());
        Assertions.assertEquals(expectedJob.getJobUuid(), jobEntity.getJobUuid());
        Assertions.assertEquals(expectedJob.getDurationMs(), jobEntity.getDurationMs());
        Assertions.assertEquals("swmp", jobEntity.getModelName());
        Assertions.assertEquals(expectedJob.getComment(), jobEntity.getComment());
        Assertions.assertNotNull(jobEntity.getCreatedTime());
        Assertions.assertNull(jobEntity.getFinishedTime());
        Assertions.assertEquals(0, jobEntity.getIsDeleted());
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
