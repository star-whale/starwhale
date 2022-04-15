package ai.starwhale.mlops.resulting.impl.clsmulti.ppl;

import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.CohenKappa;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MBCConfusionMetrics;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCConfusionMetrics;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCIndicator;
import ai.starwhale.mlops.resulting.pipline.JobResultCalculator;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class McJobResultCalculator implements JobResultCalculator<MCIndicator, Indicator> {

    @Override
    public Collection<Indicator> calculate(Collection<MCIndicator> jobLevelIndicators) {
        Map<String, List<MCIndicator>> collect = jobLevelIndicators.parallelStream()
            .collect(Collectors.groupingBy(Indicator::getName));
        final MCConfusionMetrics mcConfusionMetrics = new MCConfusionMetrics(collect);

        final CohenKappa cohenKappa = new CohenKappa(mcConfusionMetrics);

        final MBCConfusionMetrics mbcConfusionMetrics = new MBCConfusionMetrics(mcConfusionMetrics);
        return List.of(mcConfusionMetrics,cohenKappa,mbcConfusionMetrics);
    }
}
