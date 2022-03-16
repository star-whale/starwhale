/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import lombok.Data;

/**
 * Star Whale Model Package
 */
@Data
public class SWModelPackage {

    Long id;

    /**
     * The storage path of the swmp, it could be a directory or a single file
     */
    String path;

}
