package ai.starwhale.mlops.api.protocol.user;

import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "User Role object", title = "Role")
@Validated
public class UserRoleVO {

    private String id;

    private ProjectVO project;

    private RoleVO role;

}
