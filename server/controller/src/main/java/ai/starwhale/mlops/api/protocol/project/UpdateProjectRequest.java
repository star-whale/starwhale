package ai.starwhale.mlops.api.protocol.project;

import ai.starwhale.mlops.common.RegExps;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class UpdateProjectRequest {

    @JsonProperty("projectName")
    @Pattern(regexp = RegExps.PROJECT_NAME_REGEX, message = "Project name is invalid.")
    private String projectName;

    @JsonProperty("ownerId")
    private String ownerId;

    @JsonProperty("privacy")
    private String privacy;

    @JsonProperty("description")
    private String description;
}
