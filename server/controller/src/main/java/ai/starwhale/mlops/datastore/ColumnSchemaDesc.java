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
public class ColumnSchemaDesc {

    /**
     * The name of the column or the attribute.
     */
    private String name;

    /**
     * When the type is LIST, TUPLE or MAP, the index field is used to indicate the order of the elements.
     */
    private Integer index;

    /**
     * The type of the column or the attribute.
     * <p>
     * It can be one of the following values:
     * <ul>
     * <li>BOOL</li>
     * <li>INT8</li>
     * <li>INT16</li>
     * <li>INT32</li>
     * <li>INT64</li>
     * <li>FLOAT32</li>
     * <li>FLOAT64</li>
     * <li>STRING</li>
     * <li>BYTES</li>
     * <li>LIST</li>
     * <li>TUPLE</li>
     * <li>MAP</li>
     * <li>OBJECT</li>
     * </ul>
     */
    private String type;

    /**
     * The original python type. It is used by the python sdk to create objects from values. Only used when type is
     * OBJECT
     */
    private String pythonType;

    /**
     * This field represents the type of elements in the list. It is only used when type is LIST. The name field of
     * elementType is never used.
     */
    private ColumnSchemaDesc elementType;

    /**
     * This field represents the type of keys in the map. It is only used when type is MAP. The name field of
     * keyType is never used.
     */
    private ColumnSchemaDesc keyType;

    /**
     * This field represents the type of values in map. It is only used when type is MAP. The name field of
     * valueType is never used.
     */
    private ColumnSchemaDesc valueType;

    /**
     * This field describes the attributes of object type.
     * Or it describes the columns of list/tuple type.
     */
    private List<ColumnSchemaDesc> attributes;
}
