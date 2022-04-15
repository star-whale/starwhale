package ai.starwhale.mlops.resulting.pipline;

import ai.starwhale.mlops.resulting.Indicator;
import java.util.Collection;

/**
 * aggregate data level indicators to task level indicators
 */
public interface TaskResultAggregator<DI extends Indicator, TI extends Indicator> {

    /**
     * aggregate data level indicators to task level indicators
     * @param dataLevelIndicators all data level indicators of the task
     * @return indicators of one task
     */
    Collection<TI> aggregate(Collection<DI> dataLevelIndicators);
}
