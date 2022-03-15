/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.domain.swds;

import lombok.Data;

/**
 * a slice of an SWDS
 */
@Data
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
