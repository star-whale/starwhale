package ai.starwhale.mlops.resulting.repo;

import ai.starwhale.mlops.resulting.Indicator;
import java.io.IOException;
import java.util.Collection;

public interface IndicatorRepo {
    Collection<Indicator> loadResult(String resultPath) throws IOException;
}
