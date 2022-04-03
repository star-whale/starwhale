/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.resulting;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * collect result metrics from Label and InferenceResult
 */
public interface ResultCollector {

    /**
     * every collector should have an identity
     * @return the identity for the collector
     */
    String getIdentity();

    /**
     * feed the collector with one pair of label and inferenceResult
     * @param label label of the raw data
     * @param inferenceResult inference result of the raw data
     */
    void feed(InputStream label,InputStream inferenceResult);

    /**
     * feed the collector with one pair of label and inferenceResult
     * @param labelResult label & inference result
     */
    void feed(InputStream labelResult);

    /**
     * collect the results
     * @return meaningful indicators
     */
    List<Indicator> collect();

    /**
     * reload result from storage
     */
    void load() throws IOException;

    /**
     * dump result to storage
     */
    void dump() throws IOException;

}
