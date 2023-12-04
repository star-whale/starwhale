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

package ai.starwhale.mlops.api.protocol.datastore;

import ai.starwhale.mlops.datastore.ColumnType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordCellDesc {
    /**
     * The record value type.
     */
    // this long name definition is for swagger to generate the schema without naming with number extension
    @NotNull
    ColumnType dataStoreValueType;

    /**
     * This field can not be null when dataStoreValueType is scalar,
     * e.g. INT8, INT16, INT32, INT64, FLOAT32, FLOAT64, STRING, BOOL
     * and the value must be encoded scalar in string.
     */
    String scalarValue;

    /**
     * This field can not be null when dataStoreValueType is list or tuple,
     */
    // this annotation is for swagger to generate the schema
    @ArraySchema(schema = @Schema(implementation = RecordCellDesc.class))
    List<RecordCellDesc> listValue;

    /**
     * This field can not be null when dataStoreValueType is map,
     */
    // OpenAPI do not support Map<RecordWithType, RecordWithType> directly, (json or yaml only support string key)
    // so we use a wrapper class
    List<RecordCellMapItem> mapValue;

    /**
     * This field can not be null when dataStoreValueType is object,
     */
    RecordCellObject objectValue;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecordCellMapItem {
        RecordCellDesc key;
        RecordCellDesc value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecordCellObject {
        Map<String, RecordCellDesc> attrs;
        String pythonType;
    }
}
