/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.resulting;

import ai.starwhale.mlops.resulting.clsmulti.CohenKappa;
import ai.starwhale.mlops.resulting.clsmulti.MCConfusionMetrics;
import ai.starwhale.mlops.resulting.clsmulti.MCIndicator;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCohenKappa {

    final TestMCConfusionMetrics testMCConfusionMetrics = new TestMCConfusionMetrics();

    final double epsilon = 0.000001d;

    /**
     * -- --/--A--/--B--/--C--
     * --A--/--4--/--2--/--1--
     * --B--/--1--/--4--/--2--
     * --C--/--1--/--2--/--3--
     *
     * p0 = (4+4+3)/(4+2+1+1+4+2+1+2+3) = 11/20=0.55
     * pe = (7*6 + 7*8 + 6*6)/(20*20) = 0.335
     * kappa = (p0-pe)/(1-pe)=0.215/0.665=0.32330827067669172932330827067669
     */
    @Test
    public void testFeedMultiThread() {
        CohenKappa cohenKappa = new CohenKappa(new MCConfusionMetrics(new HashMap<>()));
        final List<MCIndicator> indicators = testMCConfusionMetrics.mockIndicators();
        indicators.parallelStream().forEach(cohenKappa::feed);
        cohenKappa.calculate();
        doAssertion(cohenKappa);
    }

    @Test
    public void testConstructor() {
        CohenKappa cohenKappa = new CohenKappa(
            testMCConfusionMetrics.mockMcConfusionMetrics());
        doAssertion(cohenKappa);
    }

    void doAssertion(CohenKappa cohenKappa) {
        Assertions.assertTrue(cohenKappa.getValue() - 0.32330827067669172932330827067669 < epsilon);
    }


}
