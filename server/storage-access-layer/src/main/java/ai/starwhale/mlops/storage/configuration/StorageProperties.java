package ai.starwhale.mlops.storage.configuration;

import ai.starwhale.mlops.storage.s3.S3Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.storage")
public class StorageProperties {
    String type;
    String pathPrefix;
    S3Config s3Config;
}
