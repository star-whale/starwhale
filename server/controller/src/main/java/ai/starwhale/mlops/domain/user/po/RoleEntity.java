package ai.starwhale.mlops.domain.user.po;

import ai.starwhale.mlops.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleEntity extends BaseEntity {

    private Long id;

    private String roleName;

    private String roleCode;

    private String roleDescription;
}
