package ai.starwhale.mlops.domain.project.po;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRoleEntity extends BaseEntity {

    private Long id;

    private Long userId;

    private Long roleId;

    private Long projectId;

    private UserEntity user;

    private RoleEntity role;

    private ProjectEntity project;
}
