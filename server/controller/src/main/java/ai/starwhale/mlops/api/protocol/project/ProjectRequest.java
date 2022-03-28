package ai.starwhale.mlops.api.protocol.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class ProjectRequest implements Serializable {

    @NotNull
    @JsonProperty("projectName")
    private String projectName;
}
