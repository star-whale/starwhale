/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import lombok.Builder;
import lombok.Data;

/**
 * a slice of an SWDS
 */
@Data
@Builder
public class SWDataSetSlice {

    /**
     * the swds
     */
    SWDataSet swDataSet;

    /**
     * from 0 to this.swDataSet.size
     */
    Integer start;

    /**
     * from this.start to this.swDataSet.size
     */
    Integer end;
}
