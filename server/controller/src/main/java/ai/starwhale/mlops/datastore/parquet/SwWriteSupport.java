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

import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.ColumnTypeObject;
import ai.starwhale.mlops.exception.SwProcessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;


@Slf4j
public class SwWriteSupport extends WriteSupport<Map<String, Object>> {

    private final Map<String, ColumnType> schema;
    private final String tableSchema;
    private final String metadata;
    private final Map<String, String> extraMeta;
    private RecordConsumer recordConsumer;

    public SwWriteSupport(Map<String, ColumnType> schema, Map<String, String> extraMeta,
                          String tableSchema, String metadata) {
        this.schema = schema;
        this.tableSchema = tableSchema;
        this.metadata = metadata;
        this.extraMeta = extraMeta;
    }

    @Override
    public String getName() {
        return "starwhale";
    }

    @Override
    public WriteContext init(Configuration configuration) {
        try {
            var parquetSchemaStr = new ObjectMapper().writeValueAsString(this.schema.entrySet().stream()
                    .map(entry -> entry.getValue().toColumnSchemaDesc(entry.getKey()))
                    .collect(Collectors.toList()));
            extraMeta.put(SwReadSupport.PARQUET_SCHEMA_KEY, parquetSchemaStr);
            extraMeta.put(SwReadSupport.SCHEMA_KEY, this.tableSchema);
            extraMeta.put(SwReadSupport.META_DATA_KEY, this.metadata);
            extraMeta.put(SwReadSupport.ERROR_FLAG_KEY, String.valueOf(true));

            return new WriteContext(
                    new MessageType("table", this.schema.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> entry.getValue().toParquetType(entry.getKey()))
                        .collect(Collectors.toList())),
                extraMeta);
        } catch (JsonProcessingException e) {
            throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "can not convert schema to json", e);
        }

    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(Map<String, Object> record) {
        this.recordConsumer.startMessage();
        ColumnTypeObject.writeMapValue(recordConsumer, this.schema, record);
        this.recordConsumer.endMessage();
    }
}
