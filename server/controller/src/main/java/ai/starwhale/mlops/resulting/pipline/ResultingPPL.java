package ai.starwhale.mlops.resulting.pipline;

import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.repo.IndicatorRepo;

/**
 * compress a process suit for result
 * raw data --(inference)--> file --(data calculate)-->data level indicators --(task aggregate)-->task level indicators --(job aggregate)--> job level indicators --> (job calculate) --> UI level indicators
 */
public interface ResultingPPL<DI extends Indicator, TI extends Indicator, JI extends Indicator, UI extends Indicator> {

    DataResultCalculator<DI> getDataResultCalculator();

    TaskResultAggregator<DI,TI> getTaskResultAggregator();

    JobResultAggregator<TI,JI> getJobResultAggregator();

    JobResultCalculator<JI,UI> getJobResultCalculator();

    IndicatorRepo getIndicatorRepo();

}
