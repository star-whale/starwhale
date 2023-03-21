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

package ai.starwhale.mlops.domain.dataset.index.datastore;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.Float64Value;
import ai.starwhale.mlops.datastore.type.Int64Value;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * write index to DataStore
 */
@Service
@Slf4j
public class IndexWriter {

    final DataStore dataStore;

    final ObjectMapper objectMapper;

    public IndexWriter(DataStore dataStore,
            ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
    }

    public void writeToStore(String tableName, InputStream jsonLine) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(jsonLine))) {
            var schemaMap = new HashMap<String, ColumnType>();
            List<Map<String, Object>> records = new ArrayList<>();
            String strLine;
            while ((strLine = br.readLine()) != null) {
                var indexItem = new HashMap<String, Object>();
                var valueMap = objectMapper.readValue(strLine,
                        new TypeReference<Map<String, Object>>() {
                        });
                valueMap.forEach((k, v) -> {
                    BaseValue value;
                    if (v instanceof Long || v instanceof Integer || v instanceof Byte) {
                        value = new Int64Value(((Number) v).longValue());
                    } else if (v instanceof Float || v instanceof Double) {
                        value = new Float64Value(((Number) v).doubleValue());
                    } else {
                        value = BaseValue.valueOf(v);
                    }
                    schemaMap.put(k, BaseValue.getColumnType(value));
                    indexItem.put(k, BaseValue.encode(value, false, false));
                });
                records.add(indexItem);
            }
            dataStore.update(tableName,
                    new TableSchemaDesc("id",
                            schemaMap.entrySet().stream().map(entry -> ColumnSchemaDesc.builder()
                                            .name(entry.getKey())
                                            .type(entry.getValue().name())
                                            .build())
                                    .collect(Collectors.toList())),
                    records);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.NETWORK, "error while reading _meta.jsonl", e);
        } finally {
            try {
                jsonLine.close();
            } catch (IOException e) {
                log.error("error closing inputstream for _meta.jsonl");
            }
        }
    }
}
