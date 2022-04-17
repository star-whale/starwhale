/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * index to make the access to SWDS Slices faster
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWDSIndex {

    /**
     * storage file
     */
    String storagePath;

    /**
     * block items described by this index
     */
    List<SWDSBlock> swdsBlockList;

    public SWDSIndex add(SWDSBlock swdsBlock){
        this.swdsBlockList.add(swdsBlock);
        return this;
    }


}
