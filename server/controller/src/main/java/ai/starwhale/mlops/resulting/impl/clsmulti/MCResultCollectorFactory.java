package ai.starwhale.mlops.resulting.impl.clsmulti;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class MCResultCollectorFactory {

    final StoragePathCoordinator storagePathCoordinator;

    final StorageAccessService storageAccessService;

    final ObjectMapper objectMapper;

    public MCResultCollectorFactory(StoragePathCoordinator storagePathCoordinator, StorageAccessService storageAccessService, ObjectMapper objectMapper) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.objectMapper = objectMapper;
    }

    public Optional<MCResultCollector> of(String jobId){
        try {
            return Optional.of(new MCResultCollector(jobId,storagePathCoordinator,storageAccessService,objectMapper));
        } catch (IOException e) {
            log.error("creating MCResultCollector for job failed {}",jobId);
            return Optional.empty();
        }
    }
}
