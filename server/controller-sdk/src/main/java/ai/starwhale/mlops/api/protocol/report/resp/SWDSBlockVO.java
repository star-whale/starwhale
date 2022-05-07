/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report.resp;

import ai.starwhale.mlops.domain.swds.index.SWDSDataLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWDSBlockVO {
    /**
     * the offset to the original SWDS
     */
    protected Long id;

    /**
     * how many batches does this block contains
     */
    @JsonProperty("batch")
    protected int batchAmount;

    /**
     * location of labels in this block
     */
    @JsonProperty("label")
    protected SWDSDataLocation locationLabel;

    /**
     * location of inputs in this block
     */
    @JsonProperty("data")
    protected SWDSDataLocation locationInput;

    /**
     * name for the data set
     */
    String dsName;

    /**
     * version for the data set
     */
    String dsVersion;
    public void prependDSPath(String swdsPath) {
        getLocationLabel().prependDSPath(swdsPath);
        getLocationInput().prependDSPath(swdsPath);
    }
}
