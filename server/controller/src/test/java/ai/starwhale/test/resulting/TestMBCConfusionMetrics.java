/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.resulting;

import ai.starwhale.mlops.resulting.impl.clsbi.BCConfusionMetrics;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MBCConfusionMetrics;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCConfusionMetrics;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCIndicator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMBCConfusionMetrics {

    final TestMCConfusionMetrics testMCConfusionMetrics = new TestMCConfusionMetrics();

    @Test
    public void testFeedMultiThread() {
        MBCConfusionMetrics mbcConfusionMetrics = new MBCConfusionMetrics(
            new MCConfusionMetrics(new HashMap<>()));
        final List<MCIndicator> indicators = testMCConfusionMetrics.mockIndicators();
        indicators.parallelStream().forEach(mbcConfusionMetrics::feed);
        mbcConfusionMetrics.calculate();
        doAssertion(mbcConfusionMetrics);
    }

    @Test
    public void testConstructor() {
        MBCConfusionMetrics mbcConfusionMetrics = new MBCConfusionMetrics(
            testMCConfusionMetrics.mockMcConfusionMetrics());
        doAssertion(mbcConfusionMetrics);
    }

    private void doAssertion(MBCConfusionMetrics mbcConfusionMetrics) {
        final Map<String, BCConfusionMetrics> mockBCConfusionMetrics = mockBCConfusionMetrics();
        mbcConfusionMetrics.getValue().forEach((key, value) -> Assertions.assertTrue(equals(value, mockBCConfusionMetrics.get(key))));
    }

    /**
     * A: tp:4; tn:11; fp:2; fn:3
     * B: tp:4; tn:9; fp:4; fn:3
     * C: tp:3; tn:11; fp:3; fn:3
     */
    Map<String, BCConfusionMetrics> mockBCConfusionMetrics(){
        Map<String, BCConfusionMetrics> map = new HashMap<>();
        map.put("A",new BCConfusionMetrics(4,11,2,3));
        map.put("B",new BCConfusionMetrics(4,9,4,3));
        map.put("C",new BCConfusionMetrics(3,11,3,3));
        return map;
    }

    final double epsilon = 0.000001d;
    boolean equals(BCConfusionMetrics bc1,BCConfusionMetrics bc2){
        return bc1.getAccuracy() - bc2.getAccuracy() < epsilon
            && bc1.getPrecision() - bc2.getPrecision() < epsilon
            && bc1.getRecall() - bc2.getRecall() < epsilon
            && bc1.getFn() == bc2.getFn()
            && bc1.getFp() == bc2.getFp()
            && bc1.getTn() == bc2.getTn()
            && bc1.getTp() == bc2.getTp();
    }
}
