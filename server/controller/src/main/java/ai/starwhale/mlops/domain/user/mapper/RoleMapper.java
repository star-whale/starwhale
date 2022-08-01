package ai.starwhale.mlops.domain.user.mapper;

import ai.starwhale.mlops.domain.user.po.RoleEntity;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Param;

public interface RoleMapper {

    List<RoleEntity> listRoles();

    List<RoleEntity> getRolesOfProject(@NotNull @Param("userId")Long userId,
        @NotNull @Param("projectId")Long projectId);
}
