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

package ai.starwhale.mlops.datastore.parquet;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.ColumnTypeObject;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.InitContext;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

@Slf4j
public class SwReadSupport extends ReadSupport<Map<String, Object>> {

    public static final String PARQUET_SCHEMA_KEY = "parquet_schema";

    public static final String SCHEMA_KEY = "sw_schema";

    public static final String META_DATA_KEY = "sw_meta";

    public static final String ERROR_FLAG_KEY = "error";

    @Override
    public ReadContext init(InitContext context) {
        return new ReadContext(context.getFileSchema());
    }

    @Override
    public RecordMaterializer<Map<String, Object>> prepareForRead(Configuration configuration,
            Map<String, String> metadata,
            MessageType messageType,
            ReadContext readContext) {
        var errorFlag = metadata.get(ERROR_FLAG_KEY);
        if (Boolean.parseBoolean(errorFlag)) {
            throw new SwValidationException(
                    SwValidationException.ValidSubject.DATASTORE, "the file is invalid, ignore it!");
        }
        var schemaStr = metadata.get(SCHEMA_KEY);
        if (schemaStr == null) {
            throw new SwProcessException(ErrorType.DATASTORE, "no table schema found");
        }
        var tableMetaStr = metadata.get(META_DATA_KEY);
        if (tableMetaStr == null) {
            throw new SwProcessException(ErrorType.DATASTORE, "no table meta data found");
        }
        var parquetSchemaStr = metadata.get(PARQUET_SCHEMA_KEY);
        configuration.set(SCHEMA_KEY, schemaStr);
        configuration.set(META_DATA_KEY, tableMetaStr);
        Map<String, ColumnType> schema;
        if (parquetSchemaStr != null) {
            try {
                schema = new ObjectMapper()
                        .readValue(parquetSchemaStr, new TypeReference<List<ColumnSchemaDesc>>() {
                        })
                        .stream()
                        .collect(Collectors.toMap(ColumnSchemaDesc::getName, ColumnType::fromColumnSchemaDesc));
            } catch (JsonProcessingException e) {
                throw new SwProcessException(ErrorType.DATASTORE, "failed to parse parquet schema", e);
            }
        } else {
            schema = TableSchema.fromJsonString(schemaStr).getColumnTypeMapping();
        }
        var record = new AtomicReference<Map<String, Object>>();
        //noinspection unchecked
        var converter = ColumnTypeObject.getObjectConverter(
                v -> record.set((Map<String, Object>) v),
                schema);
        return new RecordMaterializer<>() {
            @Override
            public Map<String, Object> getCurrentRecord() {
                return record.get();
            }

            @Override
            public GroupConverter getRootConverter() {
                return converter;
            }
        };
    }
}
