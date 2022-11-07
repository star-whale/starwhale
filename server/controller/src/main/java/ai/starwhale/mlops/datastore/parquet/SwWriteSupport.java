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

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnTypeObject;
import ai.starwhale.mlops.datastore.TableSchema;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

@Slf4j
public class SwWriteSupport extends WriteSupport<Map<String, Object>> {

    private final TableSchema schema;
    private final String metadata;
    private RecordConsumer recordConsumer;

    public SwWriteSupport(TableSchema schema, String metadata) {
        this.schema = schema;
        this.metadata = metadata;
    }

    @Override
    public String getName() {
        return "starwhale";
    }

    @Override
    public WriteContext init(Configuration configuration) {
        return new WriteContext(new MessageType("table",
                this.schema.getColumnSchemas().stream()
                        .sorted(Comparator.comparing(ColumnSchema::getName))
                        .map(col -> col.getType().toParquetType(col.getName()))
                        .collect(Collectors.toList())),
                Map.of(SwReadSupport.SCHEMA_KEY, this.schema.toJsonString(),
                        SwReadSupport.META_DATA_KEY, this.metadata));
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(Map<String, Object> record) {
        this.recordConsumer.startMessage();
        ColumnTypeObject.writeMapValue(recordConsumer, this.schema.getColumnTypeMapping(), record);
        this.recordConsumer.endMessage();
    }
}
