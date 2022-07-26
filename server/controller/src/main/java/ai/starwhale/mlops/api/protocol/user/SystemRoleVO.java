package ai.starwhale.mlops.api.protocol.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "System Role object", title = "Role")
@Validated
public class SystemRoleVO {

    private String id;

    private UserVO user;

    private RoleVO role;
}
