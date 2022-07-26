package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.RoleVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class RoleConvertor implements Convertor<RoleEntity, RoleVO> {

    private final IDConvertor idConvertor;

    public RoleConvertor(IDConvertor idConvertor) {
        this.idConvertor = idConvertor;
    }


    @Override
    public RoleVO convert(RoleEntity roleEntity) throws ConvertException {
        return RoleVO.builder()
            .id(idConvertor.convert(roleEntity.getId()))
            .name(roleEntity.getRoleName())
            .code(roleEntity.getRoleCode())
            .description(roleEntity.getRoleDescription())
            .build();
    }

    @Override
    public RoleEntity revert(RoleVO projectRoleVO) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
