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

package ai.starwhale.mlops.domain.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectDaoTest {

    private ProjectDao projectDao;

    private ProjectMapper projectMapper;

    @BeforeEach
    public void setUp() {
        projectMapper = mock(ProjectMapper.class);
        ProjectEntity project1 = ProjectEntity.builder()
                .id(1L).projectName("p1").ownerId(1L).isDefault(1).isDeleted(0).privacy(1)
                .projectDescription("project1")
                .build();
        ProjectEntity project2 = ProjectEntity.builder()
                .id(2L).projectName("p2").ownerId(2L).isDefault(0).isDeleted(0).privacy(0)
                .projectDescription("project2")
                .build();
        given(projectMapper.find(same(1L))).willReturn(project1);
        given(projectMapper.find(same(2L))).willReturn(project2);
        given(projectMapper.findByName(same("p1"))).willReturn(List.of(project1));
        given(projectMapper.findByName(same("p2"))).willReturn(List.of(project2));
        given(projectMapper.findByNameForUpdateAndOwner(same("p1"), any())).willReturn(project1);
        given(projectMapper.findByNameForUpdateAndOwner(same("p2"), any())).willReturn(project2);
        given(projectMapper.findExistingByNameAndOwner(any(), any()))
                .willReturn(project1);
        given(projectMapper.findExistingByNameAndOwnerName(any(), any()))
                .willReturn(project2);
        given(projectMapper.listOfUser(anyString(), any(), any()))
                .willReturn(List.of(project1, project2));
        given(projectMapper.listOfUser(same("p1"), any(), any()))
                .willReturn(List.of(project1));

        projectDao = new ProjectDao(projectMapper, new IdConverter());
    }


    @Test
    public void testFindById() {
        var res = projectDao.findById(1L);
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(1L)))
        ));
        res = projectDao.findById(3L);
        assertThat(res, nullValue());

        res = projectDao.findById(0L);
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(0L))),
                is(hasProperty("projectName", is("SYSTEM")))
        ));
    }

    @Test
    public void testGetProject() {
        var res = projectDao.getProject("1");
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(1L)))
        ));
        res = projectDao.getProject("p2");
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(2L)))
        ));
        assertThrows(SwNotFoundException.class,
                () -> projectDao.getProject("not_exist"));

    }

    @Test
    public void testGetProjectId() {
        var res = projectDao.getProjectId("0");
        assertThat(res, is(0L));

        res = projectDao.getProjectId("1");
        assertThat(res, is(1L));

        res = projectDao.getProjectId("p2");
        assertThat(res, is(2L));

        res = projectDao.getProjectId("1:p1");
        assertThat(res, is(1L));

        res = projectDao.getProjectId("starwhale:p2");
        assertThat(res, is(2L));

        assertThrows(SwNotFoundException.class,
                () -> projectDao.getProjectId("9"));

        assertThrows(SwNotFoundException.class,
                () -> projectDao.getProjectId("p9"));

    }
}
