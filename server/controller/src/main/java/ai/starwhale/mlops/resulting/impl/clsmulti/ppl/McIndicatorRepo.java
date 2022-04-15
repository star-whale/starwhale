package ai.starwhale.mlops.resulting.impl.clsmulti.ppl;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCIndicator;
import ai.starwhale.mlops.resulting.repo.ObjectStorageIndicatorRepo;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class McIndicatorRepo extends ObjectStorageIndicatorRepo {


    protected McIndicatorRepo(
        StoragePathCoordinator storagePathCoordinator,
        StorageAccessService storageAccessService,
        ObjectMapper objectMapper) {
        super(storagePathCoordinator, storageAccessService, objectMapper);
    }

    @Override
    public void saveDataLevel(Collection<Indicator> indicators, String taskId) throws IOException {
        String storagePath = this.pathForDataLevelIndicator(taskId);
        this.writeToStorage(storagePath,indicators);
    }

    @Override
    public void saveTaskLevel(Collection<Indicator> indicators, String taskId) throws IOException  {
        String storagePath = this.pathForTaskLevelIndicator(taskId);
        this.writeToStorage(storagePath,indicators);
    }

    @Override
    public void saveJobLevel(Collection<Indicator> indicators, String jobId) throws IOException  {
        String storagePath = this.pathForJobLevelIndicator(jobId);
        this.writeToStorage(storagePath,indicators);
    }

    final String line="\n";
    final String SPLITER="##SPLITER##";
    /**
     * in json line formate
     * @param indicators
     * @param jobId
     * @throws IOException
     */
    @Override
    public void saveUILevel(Collection<Indicator> indicators, String jobId) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for(Indicator indicator:indicators){
            stringBuilder.append(indicator.getClass().getName());
            stringBuilder.append(SPLITER);
            stringBuilder.append(this.objectMapper.writeValueAsString(indicator));
            stringBuilder.append(line);
        }
        this.writeToStorage(this.pathForUILevelIndicator(jobId),stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Collection<Indicator> loadDataLevel(String taskId) throws IOException {
        String storagePath = this.pathForDataLevelIndicator(taskId);
        return getIndicators(storagePath);
    }

    private List<Indicator> getIndicators(String storagePath) throws IOException {
        return this.fromStorage(storagePath,
                new TypeReference<Collection<MCIndicator>>() {
                }).parallelStream().map(mcIndicator -> (Indicator) mcIndicator)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Indicator> loadTaskLevel(String taskId) throws IOException {
        String storagePath = this.pathForTaskLevelIndicator(taskId);
        return getIndicators(storagePath);
    }

    @Override
    public Collection<Indicator> loadJobLevel(String jobId) throws IOException {
        String storagePath = this.pathForJobLevelIndicator(jobId);
        return getIndicators(storagePath);
    }

    @Override
    public Collection<Indicator> loadUILevel(String jobId) throws IOException {
        try(InputStream inputStream = this.fromStorage(this.pathForUILevelIndicator(jobId))){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            Collection<Indicator> result = new LinkedList<>();
            while((line = bufferedReader.readLine()) != null) {
                String[] split = line.split(SPLITER);
                try{
                    Indicator indicator = (Indicator)objectMapper.readValue(split[1], Class.forName(split[0]));
                    result.add(indicator);
                }catch (ClassNotFoundException e) {
                    log.error("class not found for {} while collecting result for {}",split[0],jobId,e);
                }
            }
            return result;
        }

    }
}
