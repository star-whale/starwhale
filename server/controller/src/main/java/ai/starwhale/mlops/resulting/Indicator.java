/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.resulting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * an indicator of the result such as: (TF,1) (LOSS,1.678) (Accuracy,0.96)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Indicator<T> {

    /**
     * the indicator name
     */
    protected String key;

    /**
     * the value of this indicator
     */
    protected T value;

}
