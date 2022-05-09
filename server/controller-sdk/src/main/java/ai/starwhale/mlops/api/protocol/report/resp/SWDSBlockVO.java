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
