package ai.starwhale.mlops.configuration.bean;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public StoragePathCoordinator storagePathCoordinator(StorageProperties storageProperties){
        return new StoragePathCoordinator(storageProperties.getPathPrefix());
    }
}
