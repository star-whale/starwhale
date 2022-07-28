package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.api.protocol.user.ProjectRoleVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.RoleConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class ProjectRoleConvertor implements Convertor<ProjectRoleEntity, ProjectRoleVO> {

    private final IDConvertor idConvertor;
    private final ProjectConvertor projectConvertor;
    private final RoleConvertor roleConvertor;
    private final UserConvertor userConvertor;

    public ProjectRoleConvertor(IDConvertor idConvertor,
        ProjectConvertor projectConvertor, RoleConvertor roleConvertor,
        UserConvertor userConvertor) {
        this.idConvertor = idConvertor;
        this.projectConvertor = projectConvertor;
        this.roleConvertor = roleConvertor;
        this.userConvertor = userConvertor;
    }

    @Override
    public ProjectRoleVO convert(ProjectRoleEntity entity) throws ConvertException {
        return ProjectRoleVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .project(projectConvertor.convert(entity.getProject()))
            .role(roleConvertor.convert(entity.getRole()))
            .user(userConvertor.convert(entity.getUser()))
            .build();
    }

    @Override
    public ProjectRoleEntity revert(ProjectRoleVO projectRoleVO) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
