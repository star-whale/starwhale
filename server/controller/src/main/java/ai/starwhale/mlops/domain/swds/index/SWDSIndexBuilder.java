/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
