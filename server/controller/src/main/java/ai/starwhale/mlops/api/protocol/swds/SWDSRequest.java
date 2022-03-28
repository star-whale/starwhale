package ai.starwhale.mlops.api.protocol.swds;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SWDSRequest implements Serializable {

    @NotNull
    @JsonProperty("datasetName")
    private String datasetName;

    @JsonProperty("importPath")
    private String importPath;

}
