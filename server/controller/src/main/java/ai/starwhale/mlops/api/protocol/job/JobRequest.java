package ai.starwhale.mlops.api.protocol.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class JobRequest implements Serializable {

    @NotNull
    @JsonProperty("modelVersionId")
    private String modelVersionId;

    @NotNull
    @JsonProperty("datasetVersionIds")
    private String datasetVersionIds;

    @NotNull
    @JsonProperty("baseImageId")
    private String baseImageId;

    @NotNull
    @JsonProperty("deviceId")
    private String deviceId;

    @NotNull
    @JsonProperty("deviceCount")
    private Integer deviceCount;
}
