package ai.starwhale.mlops.api.protocol.swmp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SWMPVersionRequest implements Serializable {

    @JsonProperty("importPath")
    private String importPath;
}
