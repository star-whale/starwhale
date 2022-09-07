package ai.starwhale.mlops.api.protocol.user;


import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Project Role object", title = "Role")
@Validated
public class ProjectRoleVO {

    private String id;

    private UserVO user;

    private ProjectVO project;

    private RoleVO role;

    public static ProjectRoleVO empty() {
        return new ProjectRoleVO("", UserVO.empty(), ProjectVO.empty(), RoleVO.empty());
    }
}
