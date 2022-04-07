package ai.starwhale.mlops.configuration.bean;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Autowired
    StorageProperties storageProperties;

    @Bean
    public StorageAccessService storageAccessService(StorageProperties storageProperties){
        return new StorageAccessServiceS3(storageProperties.getS3Config());
    }

    @Bean
    public StoragePathCoordinator storagePathCoordinator(StorageProperties storageProperties){
        return new StoragePathCoordinator(storageProperties.getPathPrefix());
    }
}
