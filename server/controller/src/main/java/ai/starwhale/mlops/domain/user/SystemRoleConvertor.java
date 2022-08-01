package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.SystemRoleVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class SystemRoleConvertor implements Convertor<ProjectRoleEntity, SystemRoleVO> {


    private final IDConvertor idConvertor;

    private final RoleConvertor roleConvertor;

    private final UserConvertor userConvertor;

    public SystemRoleConvertor(IDConvertor idConvertor, RoleConvertor roleConvertor,
        UserConvertor userConvertor) {
        this.idConvertor = idConvertor;
        this.roleConvertor = roleConvertor;
        this.userConvertor = userConvertor;
    }


    @Override
    public SystemRoleVO convert(ProjectRoleEntity entity) throws ConvertException {
        return SystemRoleVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .role(roleConvertor.convert(entity.getRole()))
            .user(userConvertor.convert(entity.getUser()))
            .build();
    }

    @Override
    public ProjectRoleEntity revert(SystemRoleVO systemRoleVO) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
