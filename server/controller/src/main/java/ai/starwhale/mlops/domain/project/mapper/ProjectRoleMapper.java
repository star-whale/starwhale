package ai.starwhale.mlops.domain.project.mapper;

import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Param;

public interface ProjectRoleMapper {

    List<ProjectRoleEntity> listSystemRoles();
    List<ProjectRoleEntity> listProjectRoles(@NotNull @Param("projectId") Long projectId);

    int addProjectRole(@NotNull @Param("projectRole")ProjectRoleEntity projectRole);

    int addProjectRoleByName(@NotNull @Param("userId")Long userId,
        @NotNull @Param("projectId")Long projectId,
        @NotNull @Param("roleName")String roleName);

    int deleteProjectRole(@NotNull @Param("projectRoleId")Long projectRoleId);

    int updateProjectRole(@NotNull @Param("projectRole")ProjectRoleEntity projectRole);

}
