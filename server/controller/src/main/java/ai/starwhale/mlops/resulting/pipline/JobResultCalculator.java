package ai.starwhale.mlops.resulting.pipline;

import ai.starwhale.mlops.resulting.Indicator;
import java.util.Collection;

/**
 * calculate job level indicators to user indicators
 * @param <JI>
 * @param <UI>
 */
public interface JobResultCalculator<JI extends Indicator, UI extends Indicator> {

    /**
     * calculate job level indicators to user indicators
     * @param jobLevelIndicators all job level indicators of the job
     * @return indicators of one job to user interface
     */
    Collection<UI> calculate(Collection<JI> jobLevelIndicators);

}
