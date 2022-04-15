package ai.starwhale.mlops.resulting.repo;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public abstract class ObjectStorageIndicatorRepo implements IndicatorRepo{

    final protected StoragePathCoordinator storagePathCoordinator;

    final protected StorageAccessService storageAccessService;

    final protected ObjectMapper objectMapper;

    static final String PATH_DATA_LEVEL="dataLevel";
    static final String PATH_TASK_LEVEL="taskLevel";
    static final String PATH_JOB_LEVEL="jobLevel";
    static final String PATH_UI_LEVEL="uiLevel";

    /**
     * where swmp is stored
     * %s1 = result metrics prefix
     * %s2 = level
     * %s3 = id
     */
    static final String STORAGE_PATH_FORMATTER_SWMP = "%s/%s/%s";

    protected ObjectStorageIndicatorRepo(
        StoragePathCoordinator storagePathCoordinator,
        StorageAccessService storageAccessService,
        ObjectMapper objectMapper) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.objectMapper = objectMapper;
    }

    protected String pathForDataLevelIndicator(String taskId){
        return String.format(STORAGE_PATH_FORMATTER_SWMP,storagePathCoordinator.resultMetricsPath(),PATH_DATA_LEVEL,taskId);
    }

    protected String pathForTaskLevelIndicator(String taskId){
        return String.format(STORAGE_PATH_FORMATTER_SWMP,storagePathCoordinator.resultMetricsPath(),PATH_TASK_LEVEL,taskId);
    }

    protected String pathForJobLevelIndicator(String jobId){
        return String.format(STORAGE_PATH_FORMATTER_SWMP,storagePathCoordinator.resultMetricsPath(),PATH_JOB_LEVEL,jobId);
    }

    protected String pathForUILevelIndicator(String jobId){
        return String.format(STORAGE_PATH_FORMATTER_SWMP,storagePathCoordinator.resultMetricsPath(),PATH_UI_LEVEL,jobId);
    }

    protected void writeToStorage(String path, Collection<Indicator> indicators)
        throws IOException {
        storageAccessService.put(path,objectMapper.writeValueAsBytes(indicators));
    }

    protected <T extends Indicator>  Collection<T> fromStorage(String path, TypeReference<Collection<T>> typeReference)
        throws IOException {
        return objectMapper.readValue(storageAccessService.get(path),typeReference);
    }

    protected InputStream fromStorage(String path)
        throws IOException {
        return storageAccessService.get(path);
    }

    protected void writeToStorage(String path, byte[] indicators)
        throws IOException {
        storageAccessService.put(path,indicators);
    }

}
