package ai.starwhale.mlops.resulting.repo;

import ai.starwhale.mlops.resulting.Indicator;
import java.io.IOException;
import java.util.Collection;

public interface IndicatorRepo {
    void saveDataLevel(Collection<Indicator> indicators,String taskId) throws IOException;
    void saveTaskLevel(Collection<Indicator> indicators,String taskId) throws IOException;
    void saveJobLevel(Collection<Indicator> indicators,String jobId) throws IOException;
    void saveUILevel(Collection<Indicator> indicators,String jobId) throws IOException;

    Collection<Indicator> loadDataLevel(String taskId) throws IOException;
    Collection<Indicator> loadTaskLevel(String taskId) throws IOException;
    Collection<Indicator> loadJobLevel(String jobId) throws IOException;
    Collection<Indicator> loadUILevel(String jobId) throws IOException;
}
