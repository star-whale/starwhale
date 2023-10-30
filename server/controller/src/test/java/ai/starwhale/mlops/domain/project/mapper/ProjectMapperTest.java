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
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectMemberEntity;
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
    private DatasetMapper datasetMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    UserEntity user;
    ProjectEntity project;
    ProjectEntity project2;

    @BeforeEach
    public void setUp() {
        user = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.insert(user);
        project = ProjectEntity.builder()
                .projectName("pjn")
                .projectDescription("desc")
                .overview("overview")
                .ownerId(user.getId())
                .privacy(1)
                .isDefault(1)
                .build();
        projectMapper.insert(project);
        project2 = ProjectEntity.builder().projectName("pxn2").ownerId(user.getId()).privacy(0).isDefault(0)
                .build();
        projectMapper.insert(project2);
    }

    @Test
    public void testDeleteAndRecover() {
        projectMapper.remove(project.getId());
        ProjectEntity pj = projectMapper.find(this.project.getId());
        project.setIsDeleted(1);
        validProject(project, user, pj);

        projectMapper.recover(project.getId());
        pj = projectMapper.find(this.project.getId());
        project.setIsDeleted(0);
        validProject(project, user, pj);
    }

    @Test
    public void testListProjects() {
        projectMemberMapper.insert(
                ProjectMemberEntity.builder().projectId(project2.getId()).roleId(1L).userId(user.getId()).build());
        List<ProjectEntity> projectEntities = projectMapper.listOfUser("p", user.getId(),
                new Order("id", Direction.ASC).toString());
        Assertions.assertEquals(2, projectEntities.size());
        projectEntities.forEach(pj -> validProject(pj.getId().equals(project.getId()) ? project : project2, user, pj));

        projectEntities = projectMapper.listOfUser("p",
                user.getId() + 23L,
                new Order("id", Direction.ASC).toString());
        Assertions.assertEquals(1, projectEntities.size());
        validProject(project, user, projectEntities.get(0));

        projectEntities = projectMapper.listOfUser("px",
                user.getId(),
                new Order("id", Direction.ASC).toString());
        Assertions.assertEquals(1, projectEntities.size());
        validProject(project2, user, projectEntities.get(0));

        projectEntities = projectMapper.listAll("px",
                new Order("id", Direction.ASC).toString());
        Assertions.assertEquals(1, projectEntities.size());
    }

    @Test
    public void testFindProjectByName() {
        Assertions.assertEquals(0, projectMapper.findByName("p").size());
        validProject(project, user, projectMapper.findByName(project.getProjectName()).get(0));
        validProject(project2, user, projectMapper.findByName(project2.getProjectName()).get(0));
    }

    @Test
    public void testModifyProject() {
        ProjectEntity project3 = ProjectEntity.builder()
                .projectName("pxn3")
                .ownerId(user.getId())
                .privacy(0)
                .overview("overview3") // new overview content
                .isDefault(0)
                .build();
        project3.setId(project.getId());
        projectMapper.update(project3);

        var newest = projectMapper.find(project.getId());
        validProject(project3, user, newest);
        Assertions.assertEquals(project3.getOverview(), newest.getOverview());
    }

    @Test
    public void testListObjectCounts() {
        modelMapper.insert(
                ModelEntity.builder().modelName("swmp").projectId(project.getId())
                        .ownerId(user.getId()).build());
        ModelEntity removed = ModelEntity.builder().modelName("swmp2").projectId(project.getId())
                .ownerId(user.getId()).build();
        modelMapper.insert(removed);
        modelMapper.remove(removed.getId());
        modelMapper.insert(
                ModelEntity.builder().modelName("swmp").projectId(project2.getId())
                        .ownerId(user.getId()).build());
        jobMapper.addJob(JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .projectId(project.getId()).ownerId(user.getId()).build());
        jobMapper.addJob(JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .projectId(project.getId()).ownerId(user.getId()).build());
        jobMapper.addJob(JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.PAUSED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(1L)
                .resultOutputPath("").type(JobType.EVALUATION)
                .projectId(project2.getId()).ownerId(user.getId()).build());
        datasetMapper.insert(
                DatasetEntity.builder().datasetName("dsn").projectId(project.getId()).ownerId(1L).build());
        datasetMapper.insert(
                DatasetEntity.builder().datasetName("dsn2").projectId(project.getId()).ownerId(1L).build());
        datasetMapper.insert(
                DatasetEntity.builder().datasetName("dsn3").projectId(project.getId()).ownerId(1L).build());
        datasetMapper.insert(
                DatasetEntity.builder().datasetName("dsn3").projectId(project2.getId()).ownerId(1L).build());
        DatasetEntity removedDs =
                DatasetEntity.builder().datasetName("dsn4").projectId(project2.getId()).ownerId(1L).build();
        datasetMapper.insert(removedDs);
        datasetMapper.remove(removedDs.getId());

        List<ObjectCountEntity> counts = projectMapper.countModel(String.valueOf(project.getId()));
        Assertions.assertEquals(1, counts.size());
        Assertions.assertEquals(project.getId(), counts.get(0).getProjectId());
        Assertions.assertEquals(1, counts.get(0).getCount());

        counts = projectMapper.countDataset(String.valueOf(project.getId()));
        Assertions.assertEquals(1, counts.size());
        Assertions.assertEquals(project.getId(), counts.get(0).getProjectId());
        Assertions.assertEquals(3, counts.get(0).getCount());

        counts = projectMapper.countJob(String.valueOf(project.getId()));
        Assertions.assertEquals(1, counts.size());
        Assertions.assertEquals(project.getId(), counts.get(0).getProjectId());
        Assertions.assertEquals(2, counts.get(0).getCount());

        counts = projectMapper.countMember(String.valueOf(project.getId()));
        Assertions.assertEquals(0, counts.size());

        counts = projectMapper.countRuntime(String.valueOf(project.getId()));
        Assertions.assertEquals(0, counts.size());

    }

    @Test
    public void testFindProjectByNameAndOwner() {
        ProjectEntity res = projectMapper.findExistingByNameAndOwner("pjn", user.getId());
        validProject(project, user, res);

        res = projectMapper.findExistingByNameAndOwnerName("pxn2", "un12");
        validProject(project2, user, res);
    }

    private void validProject(ProjectEntity expected, UserEntity user, ProjectEntity actual) {
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getOwnerId(), actual.getOwnerId());
        Assertions.assertEquals(expected.getProjectName(), actual.getProjectName());
        Assertions.assertEquals(expected.getProjectDescription(), actual.getProjectDescription());
        Assertions.assertEquals(null == expected.getIsDeleted() ? 0 : expected.getIsDeleted(), actual.getIsDeleted());
        Assertions.assertEquals(null == expected.getIsDefault() ? 0 : expected.getIsDefault(), actual.getIsDefault());
        Assertions.assertEquals(expected.getPrivacy(), actual.getPrivacy());
    }

    private void validUser(UserEntity expected, UserEntity actual) {
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getUserName(), actual.getUserName());
        Assertions.assertEquals(expected.getUserEnabled(), actual.getUserEnabled());
    }

}

