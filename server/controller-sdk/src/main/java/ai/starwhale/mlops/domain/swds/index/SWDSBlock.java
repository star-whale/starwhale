/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the storage unit of one data set
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWDSBlock {

    /**
     * the offset to the original SWDS
     */
    Long id;

    /**
     * how many batches does this block contains
     */
    @JsonProperty("batch")
    int batchAmount;

    /**
     * location of labels in this block
     */
    @JsonProperty("label")
    SWDSDataLocation locationLabel;

    /**
     * location of inputs in this block
     */
    @JsonProperty("data")
    SWDSDataLocation locationInput;

    public void prependDSPath(String swdsPath) {
        locationLabel.prependDSPath(swdsPath);
        locationInput.prependDSPath(swdsPath);
    }
}
