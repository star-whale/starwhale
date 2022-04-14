/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Star Whale Data Set
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWDataSet {

    /**
     * unique id of the swds
     */
    Long id;

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
