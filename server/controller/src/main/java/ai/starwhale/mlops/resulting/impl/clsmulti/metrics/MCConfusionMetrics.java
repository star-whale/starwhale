/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl.clsmulti.metrics;

import ai.starwhale.mlops.resulting.Indicator;
import cn.hutool.core.collection.ConcurrentHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;

/**
 * a Multiclass Classification confusion metrics like this:
 * -- --/--A--/--B--/--C--
 * --A--/--3--/--2--/--0--
 * --B--/--1--/--7--/--2--
 * --C--/--2--/--1--/--5--
 */
@NoArgsConstructor
public class MCConfusionMetrics extends Indicator<List<MCIndicator>> {

    public static final String NAME = "MCConfusionMetrics";

    final Map<String, MCIndicator> lookupTable = new HashMap<>();

    final Set<String> labelSet = new ConcurrentHashSet<>();

    public MCConfusionMetrics(Map<String, List<MCIndicator>> resultHolder) {
        super(NAME, null);
        this.value = resultHolder.entrySet().stream().map(entry -> {
            final String key = entry.getKey();
            final List<MCIndicator> indicators = entry.getValue();
            final int total = indicators.stream().map(Indicator::getValue)
                .mapToInt(AtomicInteger::intValue).sum();
            final MCIndicator mcIndicator = new MCIndicator(key, total);
            lookupTable.put(key, mcIndicator);
            updateLabelSet(mcIndicator);
            return mcIndicator;
        }).collect(Collectors.toList());
    }

    private void updateLabelSet(MCIndicator mcIndicator) {
        labelSet.add(mcIndicator.getLabel());
        labelSet.add(mcIndicator.getPrediction());
    }

    @Override
    public List<MCIndicator> getValue(){
        makeUpForZeros();
        return this.value;
    }

    private void makeUpForZeros() {
        MCIndicator mcIndicator;
        //do Cartesian product
        for(String l:labelSet){
            for(String l2:labelSet){
                mcIndicator = new MCIndicator(l, l2);
                String indicatorName = mcIndicator.getName();
                if(!lookupTable.containsKey(indicatorName)){
                    lookupTable.put(indicatorName,mcIndicator);
                    this.value.add(mcIndicator);
                }
            }
        }
    }




    public void feed(MCIndicator freshIndicator) {
        MCIndicator residentIndicator = lookupTable.get(freshIndicator.getName());
        if (null == residentIndicator) {
            synchronized (lookupTable) {
                residentIndicator = lookupTable.get(freshIndicator.getName());
                if (null == residentIndicator) {
                    lookupTable.put(freshIndicator.getName(), freshIndicator);
                    this.value.add(freshIndicator);
                } else {
                    residentIndicator.getValue().addAndGet(freshIndicator.getValue().get());
                }
            }
        } else {
            residentIndicator.getValue().addAndGet(freshIndicator.getValue().get());
        }
        updateLabelSet(freshIndicator);
    }


}
