/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.resulting;

import java.io.InputStream;
import java.util.List;

/**
 * collect result metrics from Label and InferenceResult
 */
public interface ResultCollector {

    /**
     * one pair of label and inferenceResult should have one indicator
     * @param label label of the raw data
     * @param inferenceResult inference result of the raw data
     * @return indicator of the inference result
     */
    Indicator compare(InputStream label,InputStream inferenceResult);

    /**
     * reduce indicators to more meaningful ones such as [(TP,1),(TP,1),(TN,1)] to (Accuracy,0.67)
     * @param indicators low level indicators
     * @return more meaningful indicators
     */
    List<Indicator> reduce(List<Indicator> indicators);
}
