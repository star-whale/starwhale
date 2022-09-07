package ai.starwhale.mlops.domain.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.api.protocol.user.ProjectRoleVO;
import ai.starwhale.mlops.api.protocol.user.RoleVO;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.RoleConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectRoleConvertorTest {

    private ProjectRoleConvertor projectRoleConvertor;

    @BeforeEach
    public void setUp() {
        IDConvertor idConvertor = new IDConvertor();
        ProjectConvertor projectConvertor = mock(ProjectConvertor.class);
        given(projectConvertor.convert(any())).willReturn(ProjectVO.empty());
        UserConvertor userConvertor = mock(UserConvertor.class);
        given(userConvertor.convert(any())).willReturn(UserVO.empty());
        RoleConvertor roleConvertor = mock(RoleConvertor.class);
        given(roleConvertor.convert(any())).willReturn(RoleVO.empty());
        projectRoleConvertor = new ProjectRoleConvertor(idConvertor, projectConvertor, roleConvertor, userConvertor);
    }

    @Test
    public void testConvert() {
        var res = projectRoleConvertor.convert(null);
        assertThat(res, allOf(
            notNullValue(),
            hasProperty("id", emptyString()),
            hasProperty("user", isA(UserVO.class)),
            hasProperty("project", isA(ProjectVO.class)),
            hasProperty("role", isA(RoleVO.class))
        ));

        res = projectRoleConvertor.convert(ProjectRoleEntity.builder()
                .id(1L)
                .user(UserEntity.builder().build())
                .project(ProjectEntity.builder().build())
                .role(RoleEntity.builder().build())
                .build());
        assertThat(res, allOf(
            notNullValue(),
            hasProperty("id", is("1")),
            hasProperty("user", isA(UserVO.class)),
            hasProperty("project", isA(ProjectVO.class)),
            hasProperty("role", isA(RoleVO.class))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(UnsupportedOperationException.class,
            () -> projectRoleConvertor.revert(ProjectRoleVO.builder().build()));
    }
}
