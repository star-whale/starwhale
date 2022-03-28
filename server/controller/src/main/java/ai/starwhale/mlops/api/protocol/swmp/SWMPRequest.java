package ai.starwhale.mlops.api.protocol.swmp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SWMPRequest implements Serializable {

    @NotNull
    @JsonProperty("modelName")
    private String modelName;

    @JsonProperty("importPath")
    private String importPath;
}
