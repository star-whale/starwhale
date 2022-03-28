package ai.starwhale.mlops.api.protocol.swds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SWDSVersionRequest {

    @JsonProperty("importPath")
    private String importPath;
}
