/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.domain.swds;

import lombok.Data;

/**
 * Star Whale Data Set
 */
@Data
public class SWDataSet {

    /**
     * The total amount data pairs of the DS
     * One data pair contains a piece of Raw Data and a piece of Label Data
     */
    Integer size;

    /**
     * The storage path of the DS, it could be a directory or a single file
     */
    String path;

    /**
     * The storage path of the DS index, it could be a directory or a single file
     */
    String indexPath;
}
