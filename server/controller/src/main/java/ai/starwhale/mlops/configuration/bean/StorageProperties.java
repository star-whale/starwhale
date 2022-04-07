package ai.starwhale.mlops.configuration.bean;

import ai.starwhale.mlops.storage.s3.S3Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    String pathPrefix;
    S3Config s3Config;
}
