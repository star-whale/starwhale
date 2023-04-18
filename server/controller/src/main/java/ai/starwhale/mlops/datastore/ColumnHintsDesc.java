/*
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

package ai.starwhale.mlops.datastore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes the schema of a column or an attribute of an object, including its name and type.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ColumnHintsDesc {

    private List<String> typeHints;

    private List<String> columnValueHints;

    /**
     * This field represents the type of elements in the list. It is only used when type is LIST. The name field of
     * elementType is never used.
     */
    private ColumnHintsDesc elementHints;

    /**
     * This field represents the type of keys in the map. It is only used when type is MAP. The name field of
     * keyType is never used.
     */
    private ColumnHintsDesc keyHints;

    /**
     * This field represents the type of values in map. It is only used when type is MAP. The name field of
     * valueType is never used.
     */
    private ColumnHintsDesc valueHints;

    /**
     * This field describes the attributes of object type.
     */
    private Map<String, ColumnHintsDesc> attributesHints;
}
