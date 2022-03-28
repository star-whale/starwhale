/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.exception.StarWhaleException;
import lombok.Data;

import java.util.function.Consumer;

/**
 * build index for a given SWDS
 */
public interface SWDSIndexBuilder {

    /**
     * build index asynchronously
     * @param swds the swds to be build
     * @param callback when the build is finished
     */
    void submitIndexBuildRequest(SWDataSet swds, Consumer<BuildResult> callback);

    /**
     * the result of index building
     */
    @Data
    class BuildResult{

        /**
         * requested swds
         */
        SWDataSet swds;

        /**
         * index storage path if the index is built success
         */
        String indexPath;

        /**
         * null represents success
         */
        StarWhaleException error;

        public boolean success(){
            return null == error;
        }

    }

}
