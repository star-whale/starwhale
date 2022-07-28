package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.UserRoleVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.project.ProjectConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class UserRoleConvertor implements Convertor<ProjectRoleEntity, UserRoleVO> {

    private final IDConvertor idConvertor;
    private final ProjectConvertor projectConvertor;
    private final RoleConvertor roleConvertor;

    public UserRoleConvertor(IDConvertor idConvertor, ProjectConvertor projectConvertor,
        RoleConvertor roleConvertor) {
        this.idConvertor = idConvertor;
        this.projectConvertor = projectConvertor;
        this.roleConvertor = roleConvertor;
    }


    @Override
    public UserRoleVO convert(ProjectRoleEntity projectRoleEntity) throws ConvertException {
        return UserRoleVO.builder()
            .id(idConvertor.convert(projectRoleEntity.getId()))
            .project(projectConvertor.convert(projectRoleEntity.getProject()))
            .role(roleConvertor.convert(projectRoleEntity.getRole()))
            .build();
    }

    @Override
    public ProjectRoleEntity revert(UserRoleVO userRoleVO) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
