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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
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
     * This field represents the main type of keys in the map. It is only used when type is MAP. The name field of
     * keyType is never used.
     */
    private ColumnSchemaDesc keyType;

    /**
     * This field represents the main type of values in map. It is only used when type is MAP. The name field of
     * valueType is never used.
     */
    private ColumnSchemaDesc valueType;

    /**
     * This field describes the attributes of object type.
     * Or it describes the columns of list/tuple type.
     */
    // https://github.com/swagger-api/swagger-core/issues/3484
    @ArraySchema(schema = @Schema(implementation = ColumnSchemaDesc.class))
    private List<ColumnSchemaDesc> attributes;

    /**
     * This field describes the sparse &lt;key, value&gt; schema pair of map type.
     */

    @Data
    @AllArgsConstructor
    public static class KeyValuePairSchema {
        private ColumnSchemaDesc keyType;
        private ColumnSchemaDesc valueType;
    }

    private Map<Integer, KeyValuePairSchema> sparseKeyValuePairSchema;

    public static ColumnSchemaDescBuilder int8() {
        return ColumnSchemaDesc.builder().type(ColumnType.INT8.name());
    }

    public static ColumnSchemaDescBuilder int16() {
        return ColumnSchemaDesc.builder().type(ColumnType.INT16.name());
    }

    public static ColumnSchemaDescBuilder int32() {
        return ColumnSchemaDesc.builder().type(ColumnType.INT32.name());
    }

    public static ColumnSchemaDescBuilder int64() {
        return ColumnSchemaDesc.builder().type(ColumnType.INT64.name());
    }

    public static ColumnSchemaDescBuilder float32() {
        return ColumnSchemaDesc.builder().type(ColumnType.FLOAT32.name());
    }

    public static ColumnSchemaDescBuilder float64() {
        return ColumnSchemaDesc.builder().type(ColumnType.FLOAT64.name());
    }

    public static ColumnSchemaDescBuilder bool() {
        return ColumnSchemaDesc.builder().type(ColumnType.BOOL.name());
    }

    public static ColumnSchemaDescBuilder bytes() {
        return ColumnSchemaDesc.builder().type(ColumnType.BYTES.name());
    }

    public static ColumnSchemaDescBuilder unknown() {
        return ColumnSchemaDesc.builder().type(ColumnType.UNKNOWN.name());
    }

    public static ColumnSchemaDescBuilder string() {
        return ColumnSchemaDesc.builder().type(ColumnType.STRING.name());
    }

    public static ColumnSchemaDescBuilder listOf(ColumnSchemaDescBuilder elementType) {
        return ColumnSchemaDesc.builder().type(ColumnType.LIST.name()).elementType(elementType.name("element").build());
    }

    public static ColumnSchemaDescBuilder tupleOf(ColumnSchemaDescBuilder elementType) {
        return ColumnSchemaDesc.builder()
                .type(ColumnType.TUPLE.name())
                .elementType(elementType.name("element").build());
    }

    public static ColumnSchemaDescBuilder mapOf(
            @NotNull ColumnSchemaDescBuilder keyType,
            @NotNull ColumnSchemaDescBuilder valueType,
            @Null Map<Integer, KeyValuePairSchema> sparseKeyValuePairSchema
    ) {
        return ColumnSchemaDesc.builder()
                .type(ColumnType.MAP.name())
                .keyType(keyType.name("key").build())
                .valueType(valueType.name("value").build())
                .sparseKeyValuePairSchema(sparseKeyValuePairSchema);
    }

    public static ColumnSchemaDescBuilder mapOf(ColumnSchemaDescBuilder keyType, ColumnSchemaDescBuilder valueType) {
        return mapOf(keyType, valueType, null);
    }

    public static ColumnSchemaDescBuilder objectOf(String pythonType, ColumnSchemaDescBuilder... attributes) {
        return ColumnSchemaDesc.builder()
                .type(ColumnType.OBJECT.name())
                .pythonType(pythonType)
                .attributes(Stream.of(attributes).map(ColumnSchemaDescBuilder::build).collect(Collectors.toList()));
    }
}
