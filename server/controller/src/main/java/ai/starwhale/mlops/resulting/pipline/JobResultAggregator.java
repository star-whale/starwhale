package ai.starwhale.mlops.resulting.pipline;

import ai.starwhale.mlops.resulting.Indicator;
import java.util.Collection;

/**
 * aggregate task level indicators to job level indicators
 * @param <TI>
 * @param <JI>
 */
public interface JobResultAggregator<TI extends Indicator, JI extends Indicator> {

    /**
     * aggregate task level indicators to job level indicators
     * @param taskLevelIndicators all task level indicators of the job
     * @return indicators of one job
     */
    Collection<JI> aggregate(Collection<TI> taskLevelIndicators);

}
