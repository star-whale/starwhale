package ai.starwhale.mlops.domain.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCountEntity;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectManagerTest {

    private ProjectManager projectManager;

    private ProjectMapper projectMapper;

    @BeforeEach
    public void setUp() {
        IDConvertor idConvertor = new IDConvertor();
        projectMapper = mock(ProjectMapper.class);
        ProjectEntity project1 = ProjectEntity.builder()
            .id(1L).projectName("p1").ownerId(1L).isDefault(1).isDeleted(0).privacy(1).description("project1")
            .build();
        ProjectEntity project2 = ProjectEntity.builder()
            .id(2L).projectName("p2").ownerId(2L).isDefault(0).isDeleted(0).privacy(0).description("project2")
            .build();
        given(projectMapper.findProject(same(1L))).willReturn(project1);
        given(projectMapper.findProject(same(2L))).willReturn(project2);
        given(projectMapper.findProjectByName(same("p1"))).willReturn(project1);
        given(projectMapper.findProjectByName(same("p2"))).willReturn(project2);
        given(projectMapper.listProjects(anyString(), any(), any(), any()))
            .willReturn(List.of(project1, project2));
        given(projectMapper.listProjects(same("p1"), any(), any(), any()))
            .willReturn(List.of(project1));

        projectManager = new ProjectManager(projectMapper, idConvertor);
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
    public void testFindDefaultProject() {
        given(projectMapper.findDefaultProject(same(1L)))
            .willReturn(ProjectEntity.builder().build());
        given(projectMapper.listProjectsByOwner(same(2L), any(), any()))
            .willReturn(List.of(ProjectEntity.builder().build()));

        var res = projectManager.findDefaultProject(1L);
        assertThat(res, notNullValue());

        res = projectManager.findDefaultProject(2L);
        assertThat(res, notNullValue());

        res = projectManager.findDefaultProject(3L);
        assertThat(res , nullValue());

        res = projectManager.findByNameOrDefault("p1", 1L);
        assertThat(res , allOf(
            notNullValue(),
            is(hasProperty("id", is(1L)))
        ));

        res = projectManager.findByNameOrDefault("none", 1L);
        assertThat(res, notNullValue());

        res = projectManager.findByNameOrDefault("none", 3L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindById() {
        var res = projectManager.findById(1L);
        assertThat(res , allOf(
            notNullValue(),
            is(hasProperty("id", is(1L)))
        ));
        res = projectManager.findById(3L);
        assertThat(res, nullValue());
    }

    @Test
    public void testExistProject() {
        var res = projectManager.existProject("p1");
        assertThat(res, is(true));

        res = projectManager.existProject("p3");
        assertThat(res, is(false));
    }

    @Test
    public void testGetObjectCountsOfProjects() {
        given(projectMapper.listObjectCounts(argThat(list -> list.contains(1L))))
            .willReturn(List.of(ProjectObjectCountEntity.builder()
                    .projectId(1L)
                    .countModel(2)
                .build()));
        var res = projectManager.getObjectCountsOfProjects(List.of(1L, 2L));
        assertThat(res, allOf(
            notNullValue(),
            is(hasKey(1L)),
            is(hasEntry(is(1L), is(hasProperty("countModel", is(2)))))
        ));

        res = projectManager.getObjectCountsOfProjects(List.of(2L));
        assertThat(res, allOf(
            notNullValue(),
            anEmptyMap()
        ));
    }

    @Test
    public void testGetProject() {
        var res = projectManager.getProject("1");
        assertThat(res , allOf(
            notNullValue(),
            is(hasProperty("id", is(1L)))
        ));
        res = projectManager.getProject("p2");
        assertThat(res , allOf(
            notNullValue(),
            is(hasProperty("id", is(2L)))
        ));
        assertThrows(StarWhaleApiException.class,
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

        assertThrows(StarWhaleApiException.class,
            () -> projectManager.getProjectId("9"));

        assertThrows(StarWhaleApiException.class,
            () -> projectManager.getProjectId("p9"));

    }
}
