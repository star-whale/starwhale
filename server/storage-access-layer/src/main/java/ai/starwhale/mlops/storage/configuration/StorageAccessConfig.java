package ai.starwhale.mlops.storage.configuration;

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAccessConfig {

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "s3", matchIfMissing = true)
    public StorageAccessService storageAccessService(StorageProperties storageProperties){
        return new StorageAccessServiceS3(storageProperties.getS3Config());
    }

}
