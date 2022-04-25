package ai.starwhale.mlops.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "sw.controller")
public class ControllerProperties {
    private String apiPrefix = "/api/v1";
    private List<String> whiteList;
}
