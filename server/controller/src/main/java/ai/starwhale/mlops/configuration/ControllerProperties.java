package ai.starwhale.mlops.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.controller")
public class ControllerProperties {
    private String apiPrefix = "/api/v1";
}
