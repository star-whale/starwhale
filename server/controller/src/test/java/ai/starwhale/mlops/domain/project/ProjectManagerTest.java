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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectManagerTest {

    private ProjectManager projectManager;

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
        given(projectMapper.list(anyString(), any(), any()))
                .willReturn(List.of(project1, project2));
        given(projectMapper.list(same("p1"), any(), any()))
                .willReturn(List.of(project1));

        projectManager = new ProjectManager(projectMapper, new IdConverter());
    }

    @Test
    public void testListProject() {
        var res = projectManager.listProjects("", 1L, OrderParams.builder().build());
        assertThat(res, allOf(
                notNullValue(),
                iterableWithSize(2)
        ));

        res = projectManager.listProjects("p1", 1L, OrderParams.builder().build());
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("id", is(1L))))
        ));
    }

    @Test
    public void testFindById() {
        var res = projectManager.findById(1L);
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(1L)))
        ));
        res = projectManager.findById(3L);
        assertThat(res, nullValue());
    }

    @Test
    public void testExistProject() {
        var res = projectManager.existProject("p1", 1L);
        assertThat(res, is(true));

        res = projectManager.existProject("p3", 1L);
        assertThat(res, is(false));
    }

    @Test
    public void testGetObjectCountsOfProjects() {
        given(projectMapper.countModel(anyString()))
                .willReturn(List.of(
                        ObjectCountEntity.builder().projectId(1L).count(2).build(),
                        ObjectCountEntity.builder().projectId(2L).count(3).build()
                ));
        given(projectMapper.countDataset(anyString()))
                .willReturn(List.of(
                        ObjectCountEntity.builder().projectId(1L).count(4).build()
                ));
        given(projectMapper.countRuntime(anyString()))
                .willReturn(List.of(
                        ObjectCountEntity.builder().projectId(2L).count(5).build()
                ));
        given(projectMapper.countJob(anyString()))
                .willReturn(List.of(
                        ObjectCountEntity.builder().projectId(1L).count(6).build(),
                        ObjectCountEntity.builder().projectId(2L).count(7).build(),
                        ObjectCountEntity.builder().projectId(3L).count(8).build()
                ));
        given(projectMapper.countMember(anyString()))
                .willReturn(List.of(
                        ObjectCountEntity.builder().projectId(1L).count(9).build(),
                        ObjectCountEntity.builder().projectId(2L).count(10).build()
                ));

        var res = projectManager.getObjectCountsOfProjects(List.of(1L, 2L));
        assertThat(res, allOf(
                notNullValue(),
                is(hasKey(1L)),
                is(hasKey(2L)),
                is(hasEntry(is(1L), is(
                        allOf(
                                hasProperty("countModel", is(2)),
                                hasProperty("countDataset", is(4)),
                                hasProperty("countRuntime", is(0)),
                                hasProperty("countJob", is(6)),
                                hasProperty("countMember", is(9))
                        )
                ))),
                is(hasEntry(is(2L), is(
                        allOf(
                                hasProperty("countModel", is(3)),
                                hasProperty("countDataset", is(0)),
                                hasProperty("countRuntime", is(5)),
                                hasProperty("countJob", is(7)),
                                hasProperty("countMember", is(10))
                        )
                )))
        ));
    }

    @Test
    public void testGetProject() {
        var res = projectManager.getProject("1");
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(1L)))
        ));
        res = projectManager.getProject("p2");
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(2L)))
        ));
        assertThrows(StarwhaleApiException.class,
                () -> projectManager.getProject("not_exist"));

    }

    @Test
    public void testGetProjectId() {
        var res = projectManager.getProjectId("0");
        assertThat(res, is(0L));

        res = projectManager.getProjectId("1");
        assertThat(res, is(1L));

        res = projectManager.getProjectId("p2");
        assertThat(res, is(2L));

        res = projectManager.getProjectId("1:p1");
        assertThat(res, is(1L));

        res = projectManager.getProjectId("starwhale:p2");
        assertThat(res, is(2L));

        assertThrows(StarwhaleApiException.class,
                () -> projectManager.getProjectId("9"));

        assertThrows(StarwhaleApiException.class,
                () -> projectManager.getProjectId("p9"));

    }
}
