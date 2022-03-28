package ai.starwhale.mlops.api.protocol.swmp;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class RevertSWMPVersionRequest {

    @NotNull
    @JsonProperty("versionId")
    private String versionId;
}
