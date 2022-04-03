/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl.clsmulti;

import ai.starwhale.mlops.resulting.Indicator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * a Multiclass Classification confusion metrics like this:
 * -- --/--A--/--B--/--C--
 * --A--/--3--/--2--/--0--
 * --B--/--1--/--7--/--2--
 * --C--/--2--/--1--/--5--
 */
public class MCConfusionMetrics extends Indicator<List<MCIndicator>> {

    public static final String NAME = "MCConfusionMetrics";

    final Map<String, MCIndicator> lookupTable = new HashMap<>();

    public MCConfusionMetrics(Map<String, List<MCIndicator>> resultHolder) {
        super(NAME, null);
        this.value = resultHolder.entrySet().stream().map(entry -> {
            final String key = entry.getKey();
            final List<MCIndicator> indicators = entry.getValue();
            final int total = indicators.stream().map(Indicator::getValue)
                .mapToInt(AtomicInteger::intValue).sum();
            final MCIndicator mcIndicator = new MCIndicator(key, total);
            lookupTable.put(key, mcIndicator);
            return mcIndicator;
        }).collect(Collectors.toList());
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
    }


}
