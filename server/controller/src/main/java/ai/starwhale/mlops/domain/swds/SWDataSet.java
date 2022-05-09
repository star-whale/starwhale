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

package ai.starwhale.mlops.domain.swds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Star Whale Data Set
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWDataSet {

    /**
     * unique id of the swds
     */
    Long id;

    /**
     * The total amount data pairs of the DS
     * One data pair contains a piece of Raw Data and a piece of Label Data
     */
    Integer size;

    /**
     * The storage path of the DS, it could be a directory or a single file
     */
    String path;

    /**
     * The storage path of the DS index, it could be a directory or a single file
     */
    String indexPath;

    /**
     * the name for the data set
     */
    String name;

    /**
     * the version for the data set
     */
    String version;
}
