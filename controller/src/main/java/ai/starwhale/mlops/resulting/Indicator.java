/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.resulting;

import lombok.Data;

/**
 * an indicator of the result such as: (TF,1) (LOSS,1.678) (Accuracy,0.96)
 */
@Data
public class Indicator {

    /**
     * the indicator name
     */
    String key;

    /**
     * the value of this indicator
     */
    Double value;
}
