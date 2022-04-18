package ai.starwhale.mlops.resulting.impl.clsmulti.ppl;

import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.repo.IndicatorRepo;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCIndicator;
import ai.starwhale.mlops.resulting.pipline.DataResultCalculator;
import ai.starwhale.mlops.resulting.pipline.JobResultAggregator;
import ai.starwhale.mlops.resulting.pipline.JobResultCalculator;
import ai.starwhale.mlops.resulting.pipline.ResultingPPL;
import ai.starwhale.mlops.resulting.pipline.TaskResultAggregator;
import org.springframework.stereotype.Component;

@Component
public class MCResultingPPL implements ResultingPPL<MCIndicator,MCIndicator,MCIndicator, Indicator> {

    final McDataResultCalculator mcResultCalculator;

    final McIndicatorReducer mcIndicatorReducer;

    final McJobResultCalculator mcJobResultCalculator;

    final McIndicatorRepo mcIndicatorRepo;

    public MCResultingPPL(
        McDataResultCalculator mcResultCalculator,
        McIndicatorReducer mcIndicatorReducer,
        McJobResultCalculator mcJobResultCalculator,
        McIndicatorRepo mcIndicatorRepo) {
        this.mcResultCalculator = mcResultCalculator;
        this.mcIndicatorReducer = mcIndicatorReducer;
        this.mcJobResultCalculator = mcJobResultCalculator;
        this.mcIndicatorRepo = mcIndicatorRepo;
    }

    @Override
    public String getUniqueName() {
        return "MCResultCollector";
    }

    @Override
    public DataResultCalculator<MCIndicator> getDataResultCalculator() {
        return mcResultCalculator;
    }

    @Override
    public TaskResultAggregator<MCIndicator,MCIndicator> getTaskResultAggregator() {
        return mcIndicatorReducer;
    }

    @Override
    public JobResultAggregator<MCIndicator,MCIndicator> getJobResultAggregator() {
        return mcIndicatorReducer;
    }

    @Override
    public JobResultCalculator<MCIndicator,Indicator> getJobResultCalculator() {
        return mcJobResultCalculator;
    }

    @Override
    public IndicatorRepo getIndicatorRepo() {
        return mcIndicatorRepo;
    }
}
