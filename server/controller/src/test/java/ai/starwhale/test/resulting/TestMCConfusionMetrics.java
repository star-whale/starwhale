/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.resulting;

import ai.starwhale.mlops.resulting.clsmulti.MCConfusionMetrics;
import ai.starwhale.mlops.resulting.clsmulti.MCIndicator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMCConfusionMetrics {

    @Test
    public void testFeedMultiThread() {
        MCConfusionMetrics mcConfusionMetrics = mockMcConfusionMetrics();
        doAssertion(mcConfusionMetrics);
    }

    @Test
    public void testConstructor(){
        doAssertion(new MCConfusionMetrics(mockResultHolder()));
    }

    private void doAssertion(MCConfusionMetrics mcConfusionMetrics) {
        final List<MCIndicator> metricsValue = mcConfusionMetrics.getValue();
        Assertions.assertEquals(9, metricsValue.size());
        Map<String, Integer> metricsExpected = mockMetrics();
        metricsValue.forEach(mcIndicator -> Assertions.assertEquals(metricsExpected.get(mcIndicator.getKey()),
            mcIndicator.getValue().intValue()));
    }

    public MCConfusionMetrics mockMcConfusionMetrics() {
        MCConfusionMetrics mcConfusionMetrics = new MCConfusionMetrics(new HashMap<>());
        final List<MCIndicator> indicators = mockIndicators();
        indicators.parallelStream().forEach(mcConfusionMetrics::feed);
        return mcConfusionMetrics;
    }

    Map<String, List<MCIndicator>> mockResultHolder() {
        Map<String, List<MCIndicator>> mockResultHolder = new HashMap<>();
        mockResultHolder.put("A-A", new LinkedList<>());
        mockResultHolder.put("A-B", new LinkedList<>());
        mockResultHolder.put("A-C", new LinkedList<>());
        mockResultHolder.put("B-A", new LinkedList<>());
        mockResultHolder.put("B-B", new LinkedList<>());
        mockResultHolder.put("B-C", new LinkedList<>());
        mockResultHolder.put("C-A", new LinkedList<>());
        mockResultHolder.put("C-B", new LinkedList<>());
        mockResultHolder.put("C-C", new LinkedList<>());
        final List<MCIndicator> indicators = mockIndicators();
        indicators.forEach(indicator-> mockResultHolder.get(indicator.getKey()).add(indicator));
        return mockResultHolder;
    }

    Map<String, Integer> mockMetrics() {
        Map<String, Integer> metricsExpected = new HashMap<>();
        metricsExpected.put("A-A", 4);
        metricsExpected.put("A-B", 2);
        metricsExpected.put("A-C", 1);
        metricsExpected.put("B-A", 1);
        metricsExpected.put("B-B", 4);
        metricsExpected.put("B-C", 2);
        metricsExpected.put("C-A", 1);
        metricsExpected.put("C-B", 2);
        metricsExpected.put("C-C", 3);
        return metricsExpected;
    }

    /**
     * -- --/--A--/--B--/--C--
     * --A--/--4--/--2--/--1--
     * --B--/--1--/--4--/--2--
     * --C--/--1--/--2--/--3--
     */
    List<MCIndicator> mockIndicators() {
        List<MCIndicator> indicators = new LinkedList<>();
        indicators.add(new MCIndicator("A", "A"));
        indicators.add(new MCIndicator("A", "A"));
        indicators.add(new MCIndicator("A", "A"));
        indicators.add(new MCIndicator("A", "A"));
        indicators.add(new MCIndicator("A", "B"));
        indicators.add(new MCIndicator("A", "B"));
        indicators.add(new MCIndicator("A", "C"));
        indicators.add(new MCIndicator("B", "A"));
        indicators.add(new MCIndicator("B", "B"));
        indicators.add(new MCIndicator("B", "B"));
        indicators.add(new MCIndicator("B", "B"));
        indicators.add(new MCIndicator("B", "B"));
        indicators.add(new MCIndicator("B", "C"));
        indicators.add(new MCIndicator("B", "C"));
        indicators.add(new MCIndicator("C", "A"));
        indicators.add(new MCIndicator("C", "B"));
        indicators.add(new MCIndicator("C", "B"));
        indicators.add(new MCIndicator("C", "C"));
        indicators.add(new MCIndicator("C", "C"));
        indicators.add(new MCIndicator("C", "C"));
        return indicators;
    }
}
