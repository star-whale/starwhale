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

package ai.starwhale.mlops.domain.project.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetMapper;
import ai.starwhale.mlops.domain.swds.po.SwDatasetEntity;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
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
public class ProjectMapperTest extends MySqlContainerHolder {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private SwDatasetMapper swDatasetMapper;

    @Autowired
    private SwModelPackageMapper swModelPackageMapper;

    @Autowired
    private ProjectRoleMapper projectRoleMapper;

    UserEntity user;
    ProjectEntity project;
    ProjectEntity project2;

    @BeforeEach
    public void setUp() {
        user = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.createUser(user);
        project = ProjectEntity.builder().projectName("pjn").ownerId(user.getId()).privacy(1).isDefault(1)
                .build();
        projectMapper.createProject(project);
        project2 = ProjectEntity.builder().projectName("pxn2").ownerId(user.getId()).privacy(0).isDefault(0)
                .build();
        projectMapper.createProject(project2);
    }

    @Test
    public void testDeleteAndRecover() {
        projectMapper.deleteProject(project.getId());
        ProjectEntity pj = projectMapper.findProject(this.project.getId());
        project.setIsDeleted(1);
        validProject(project, user, pj);

        projectMapper.recoverProject(project.getId());
        pj = projectMapper.findProject(this.project.getId());
        project.setIsDeleted(0);
        validProject(project, user, pj);
    }

    @Test
    public void testListProjects() {
        projectRoleMapper.addProjectRole(
                ProjectRoleEntity.builder().projectId(project2.getId()).roleId(1L).userId(user.getId()).build());
        List<ProjectEntity> projectEntities = projectMapper.listProjects("p",
                new Order("project_id", Direction.ASC).toString(),
                0, user.getId());
        Assertions.assertEquals(2, projectEntities.size());
        projectEntities.forEach(pj -> validProject(pj.getId().equals(project.getId()) ? project : project2, user, pj));

        projectEntities = projectMapper.listProjects("p", new Order("project_id", Direction.ASC).toString(),
                0, user.getId() + 23L);
        Assertions.assertEquals(1, projectEntities.size());
        validProject(project, user, projectEntities.get(0));

        projectEntities = projectMapper.listProjects("px", new Order("project_id", Direction.ASC).toString(),
                0, user.getId());
        Assertions.assertEquals(1, projectEntities.size());
        validProject(project2, user, projectEntities.get(0));


    }

    @Test
    public void testListProjectsByOwner() {
        List<ProjectEntity> projectEntities = projectMapper.listProjectsByOwner(user.getId(),
                new Order("project_id", Direction.ASC).toString(),
                0);
        Assertions.assertEquals(2, projectEntities.size());
        projectEntities.forEach(pj -> validProject(pj.getId().equals(project.getId()) ? project : project2, user, pj));

        projectEntities = projectMapper.listProjectsByOwner(user.getId() + 12L,
                new Order("project_id", Direction.ASC).toString(),
                0);
        Assertions.assertEquals(0, projectEntities.size());
    }

    @Test
    public void testFindProjectByName() {
        Assertions.assertNull(projectMapper.findProjectByName("p"));
        validProject(project, user, projectMapper.findProjectByName(project.getProjectName()));
        validProject(project2, user, projectMapper.findProjectByName(project2.getProjectName()));
    }

    @Test
    public void testFindDefaultProject() {
        validProject(project, user, projectMapper.findDefaultProject(user.getId()));
    }

    @Test
    public void testModifyProject() {
        ProjectEntity project3 = ProjectEntity.builder().projectName("pxn3").ownerId(user.getId()).privacy(0)
                .isDefault(0)
                .build();
        project3.setId(project.getId());
        projectMapper.modifyProject(project3);
        validProject(project3, user, projectMapper.findProject(project.getId()));
    }

    @Test
    public void testListObjectCounts() {
        swModelPackageMapper.addSwModelPackage(
                SwModelPackageEntity.builder().swmpName("swmp").projectId(project.getId())
                        .ownerId(user.getId()).build());
        swModelPackageMapper.addSwModelPackage(
                SwModelPackageEntity.builder().swmpName("swmp").projectId(project2.getId())
                        .ownerId(user.getId()).build());
        jobMapper.addJob(JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePoolId(1L).runtimeVersionId(1L).swmpVersionId(1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .projectId(project.getId()).ownerId(user.getId()).build());
        jobMapper.addJob(JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePoolId(1L).runtimeVersionId(1L).swmpVersionId(1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .projectId(project.getId()).ownerId(user.getId()).build());
        jobMapper.addJob(JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePoolId(1L).runtimeVersionId(1L).swmpVersionId(1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .projectId(project2.getId()).ownerId(user.getId()).build());
        swDatasetMapper.addDataset(
                SwDatasetEntity.builder().datasetName("dsn").projectId(project.getId()).ownerId(1L).build());
        swDatasetMapper.addDataset(
                SwDatasetEntity.builder().datasetName("dsn2").projectId(project.getId()).ownerId(1L).build());
        swDatasetMapper.addDataset(
                SwDatasetEntity.builder().datasetName("dsn3").projectId(project.getId()).ownerId(1L).build());
        swDatasetMapper.addDataset(
                SwDatasetEntity.builder().datasetName("dsn3").projectId(project2.getId()).ownerId(1L).build());

        List<ProjectObjectCountEntity> projectObjectCountEntities = projectMapper.listObjectCounts(
                List.of(project.getId()));
        Assertions.assertEquals(1, projectObjectCountEntities.size());
        ProjectObjectCountEntity projectObjectCountEntity = projectObjectCountEntities.get(0);
        Assertions.assertEquals(project.getId(), projectObjectCountEntity.getProjectId());
        Assertions.assertEquals(2, projectObjectCountEntity.getCountJobs());
        Assertions.assertEquals(3, projectObjectCountEntity.getCountDataset());
        Assertions.assertEquals(1, projectObjectCountEntity.getCountModel());
        Assertions.assertEquals(0, projectObjectCountEntity.getCountMember());


    }

    private void validProject(ProjectEntity expected, UserEntity user, ProjectEntity actual) {
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getOwnerId(), actual.getOwnerId());
        Assertions.assertEquals(expected.getProjectName(), actual.getProjectName());
        Assertions.assertEquals(expected.getDescription(), actual.getDescription());
        Assertions.assertEquals(null == expected.getIsDeleted() ? 0 : expected.getIsDeleted(), actual.getIsDeleted());
        Assertions.assertEquals(null == expected.getIsDefault() ? 0 : expected.getIsDefault(), actual.getIsDefault());
        Assertions.assertEquals(expected.getPrivacy(), actual.getPrivacy());
        validUser(user, actual.getOwner());
    }

    private void validUser(UserEntity expected, UserEntity actual) {
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getUserName(), actual.getUserName());
        Assertions.assertEquals(expected.getUserEnabled(), actual.getUserEnabled());
    }

}

