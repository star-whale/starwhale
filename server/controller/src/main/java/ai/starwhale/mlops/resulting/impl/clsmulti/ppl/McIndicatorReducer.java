package ai.starwhale.mlops.resulting.impl.clsmulti.ppl;

import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCIndicator;
import ai.starwhale.mlops.resulting.pipline.JobResultAggregator;
import ai.starwhale.mlops.resulting.pipline.TaskResultAggregator;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class McIndicatorReducer implements TaskResultAggregator<MCIndicator,MCIndicator>,
    JobResultAggregator<MCIndicator,MCIndicator> {

    @Override
    public Collection<MCIndicator> aggregate(Collection<MCIndicator> lastLevelIndicators) {
        return lastLevelIndicators.parallelStream()
            .collect(Collectors.groupingBy(Indicator::getName))
            .entrySet().parallelStream()
            .map(entry-> reduce(entry.getKey(),entry.getValue()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public MCIndicator reduce(String name,List<MCIndicator> mcIndicatorsWithSameName){
        if(CollectionUtils.isEmpty(mcIndicatorsWithSameName)){
            return null;
        }
        MCIndicator mcIndicator = new MCIndicator(name,0);
        mcIndicatorsWithSameName.parallelStream().forEach(mci->{
            mcIndicator.getValue().addAndGet(mci.getValue().intValue());
        });
        return mcIndicator;
    }

}
