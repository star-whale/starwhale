/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index.mnist;

import lombok.Data;

/**
 * parse mnist file paths such as t10k-labels-idx1-ubyte.gz/ t10k-images-idx3-ubyte.gz/ t10k-labels.idx1-ubyte
 */
@Data
class MNISTFile {

    final Boolean label;
    final Boolean ziped;
    final String path;

    static final String MARKER_LABEL="labels";
    static final String MARKER_GZ=".gz";

    public MNISTFile(String filePath){
        this.path = filePath;
        this.label = filePath.contains(MARKER_LABEL);
        this.ziped = filePath.endsWith(MARKER_GZ);

    }

}
