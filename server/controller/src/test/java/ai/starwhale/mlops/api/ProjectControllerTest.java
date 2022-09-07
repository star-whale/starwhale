package ai.starwhale.mlops.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.api.protocol.project.CreateProjectRequest;
import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.api.protocol.project.UpdateProjectRequest;
import ai.starwhale.mlops.api.protocol.user.ProjectRoleVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ProjectControllerTest {

    private ProjectController controller;
    private ProjectService projectService;

    @BeforeEach
    public void setUp() {
        projectService = mock(ProjectService.class);
        UserService userService = mock(UserService.class);
        given(userService.currentUserDetail()).willReturn(User.builder()
                .name("starwhale")
                .id(1L)
                .idTableKey(1L)
                .roles(Set.of(Role.builder().roleName("Owner").roleCode("OWNER").build()))
            .build());
        IDConvertor idConvertor = new IDConvertor();
        controller = new ProjectController(projectService, userService, idConvertor);
    }

    @Test
    public void testListProject() {
        given(projectService.listProject(anyString(), any(PageParams.class), any(OrderParams.class), any(User.class)))
            .willReturn(new PageInfo<>(List.of(
                ProjectVO.builder().name("test1").id("1").build(),
                ProjectVO.builder().name("test2").id("2").build(),
                ProjectVO.builder().name("test3").id("3").build()
            )));
        given(projectService.listProject(same("test1"), any(PageParams.class), any(OrderParams.class), any(User.class)))
            .willReturn(new PageInfo<>(List.of(
                ProjectVO.builder().id("1").build()
            )));
        given(projectService.listProject(same("test2"), any(PageParams.class), any(OrderParams.class), any(User.class)))
            .willReturn(new PageInfo<>(List.of(
                ProjectVO.builder().id("2").build()
            )));
        given(projectService.listProject(anyString(), argThat(page -> page.getPageNum() == 2 && page.getPageSize() == 2),
            any(OrderParams.class), any(User.class))).willReturn(new PageInfo<>(List.of(
            ProjectVO.builder().id("3").build()
        )));

        var resp = controller.listProject(
            "", 1, 10, "", 1);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
            notNullValue(),
            is(hasSize(3))
        ));

        resp = controller.listProject(
            "test1", 1, 10, "", 1);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
            notNullValue(),
            is(iterableWithSize(1)),
            is(hasItem(hasProperty("id", is("1"))))
        ));

        resp = controller.listProject(
            "test2", 1, 10, "", 1);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
            notNullValue(),
            is(iterableWithSize(1)),
            is(hasItem(hasProperty("id", is("2"))))
        ));

        resp = controller.listProject(
            "", 2, 2, "", 1);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
            notNullValue(),
            is(iterableWithSize(1)),
            is(hasItem(hasProperty("id", is("3"))))
        ));

    }

    @Test
    public void testCreateProject() {
        given(projectService.createProject(any(Project.class)))
            .willReturn(1L);
        given(projectService.createProject(argThat(p -> p.getName() == null)))
            .willThrow(StarWhaleApiException.class);

        CreateProjectRequest request = new CreateProjectRequest();
        request.setOwnerId("1");
        request.setProjectName("project1");
        request.setPrivacy("PUBLIC");
        var resp = controller.createProject(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is("1"));

        request.setProjectName(null);
        assertThrows(StarWhaleApiException.class,
            () -> controller.createProject(request));
    }

    @Test
    public void testDeleteProject() {
        given(projectService.deleteProject(anyString()))
            .willReturn(true);
        given(projectService.deleteProject(same("")))
            .willReturn(false);
        given(projectService.deleteProject(isNull()))
            .willThrow(StarWhaleApiException.class);
        var resp = controller.deleteProjectByUrl("project1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarWhaleApiException.class,
            () -> controller.deleteProjectByUrl(""));

        assertThrows(StarWhaleApiException.class,
            () -> controller.deleteProjectByUrl(null));
    }

    @Test
    public void testRecoverProject() {
        given(projectService.recoverProject(same("1")))
            .willReturn(1L);
        given(projectService.recoverProject(same("")))
            .willThrow(StarWhaleApiException.class);

        var resp = controller.recoverProject("1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarWhaleApiException.class,
            () -> controller.recoverProject(""));
    }

    @Test
    public void testGetProject() {
        String name = "project1";
        given(projectService.findProject(same(name)))
            .willReturn(ProjectVO.builder().name(name).build());
        given(projectService.findProject(same("")))
            .willThrow(StarWhaleApiException.class);

        var resp = controller.getProjectByUrl(name);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
            notNullValue(),
            is(hasProperty("name", is(name)))
        ));

        assertThrows(StarWhaleApiException.class,
            () -> controller.getProjectByUrl(""));
    }

    @Test
    public void testUpdateProject() {
        given(projectService.modifyProject(anyString(), any(), any(), any(), any()))
            .willReturn(true);
        String err_url = "err_url";
        given(projectService.modifyProject(same(err_url), any(), any(), any(), any()))
            .willReturn(false);
        String err_name = "err_name";
        given(projectService.modifyProject(anyString(), same(err_name), any(), any(), any()))
            .willThrow(StarWhaleApiException.class);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setProjectName("pro");
        var resp = controller.updateProject("project1",
            request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setProjectName(err_name);
        assertThrows(StarWhaleApiException.class,
            () -> controller.updateProject("project1", request));

        assertThrows(StarWhaleApiException.class,
            () -> controller.updateProject(err_url, request));

    }

    @Test
    public void testListProjectRole() {
        given(projectService.listProjectRoles(same("p1")))
            .willReturn(List.of(ProjectRoleVO.builder().id("1").build()));
        given(projectService.listProjectRoles(same("p2")))
            .willReturn(List.of(ProjectRoleVO.builder().id("2").build()));
        given(projectService.listProjectRoles(isNull()))
            .willThrow(StarWhaleApiException.class);

        var resp = controller.listProjectRole("p1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
            notNullValue(),
            is(hasItem(hasProperty("id", is("1"))))
        ));

        resp = controller.listProjectRole("p2");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
            notNullValue(),
            is(hasItem(hasProperty("id", is("2"))))
        ));

        assertThrows(StarWhaleApiException.class,
            () -> controller.listProjectRole(null));

    }

    @Test
    public void testAddProjectRole() {
        given(projectService.addProjectRole(anyString(), anyLong(), anyLong()))
            .willReturn(true);
        String err_url = "err_url";
        given(projectService.addProjectRole(same(err_url), anyLong(), anyLong()))
            .willThrow(StarWhaleApiException.class);

        var resp = controller.addProjectRole("p1", "1", "1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarWhaleApiException.class,
            () -> controller.addProjectRole(err_url, "1", "1"));
    }

    @Test
    public void testDeleteProjectRole() {
        given(projectService.deleteProjectRole(anyString(), anyLong()))
            .willReturn(true);
        String err_url = "err_url";
        given(projectService.deleteProjectRole(same(err_url), anyLong()))
            .willThrow(StarWhaleApiException.class);

        var resp = controller.deleteProjectRole("p1", "1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarWhaleApiException.class,
            () -> controller.deleteProjectRole(err_url, "1"));
    }

    @Test
    public void testModifyProjectRole() {
        given(projectService.modifyProjectRole(anyString(), anyLong(), anyLong()))
            .willReturn(true);
        String err_url = "err_url";
        given(projectService.modifyProjectRole(same(err_url), anyLong(), anyLong()))
            .willThrow(StarWhaleApiException.class);

        var resp = controller.modifyProjectRole("p1", "1", "2");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarWhaleApiException.class,
            () -> controller.modifyProjectRole(err_url, "1", "2"));
    }
}
