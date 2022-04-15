package ai.starwhale.mlops.resulting.pipline;

import ai.starwhale.mlops.resulting.Indicator;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * calculate data level indicators
 */
public interface DataResultCalculator<T extends Indicator> {

    /**
     *
     * @param is this method won't clos inputstream
     * @return standard results for label - prediction
     */
    List<T> calculate(InputStream is) throws IOException;
}
