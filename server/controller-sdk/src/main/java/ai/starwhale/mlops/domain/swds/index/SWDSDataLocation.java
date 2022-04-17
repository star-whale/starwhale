/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the location of the block data in one file
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWDSDataLocation {

    /**
     * the offset to the file
     */
    int offset;

    /**
     * the byte size of this block
     */
    int size;

    /**
     * the file path where this block exists
     */
    String file;

    static final String PATH_SPLITER="/";
    public void prependDSPath(String swdsPath) {
        this.file = swdsPath + PATH_SPLITER + this.file;
    }

}
