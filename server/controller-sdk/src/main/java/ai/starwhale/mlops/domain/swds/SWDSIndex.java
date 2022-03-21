/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * index to make the access to SWDS Slices faster
 */
@Data
@Builder
public class SWDSIndex {

    /**
     * storage file
     */
    String storagePath;

    /**
     * block items described by this index
     */
    List<SWDSBlock> SWDSBlockList = new LinkedList<>();

    public SWDSIndex add(SWDSBlock swdsBlock){
        this.SWDSBlockList.add(swdsBlock);
        return this;
    }

    @Data
    @Builder
    public static class SWDSBlock {

        /**
         * the offset to the original SWDS
         */
        int offset;

        /**
         * how many data pairs does this block contains
         */
        int size;

        /**
         * corresponding to pathImage
         */
        String pathLabel;

        /**
         * corresponding to pathLabel
         */
        String pathImage;
    }

}
