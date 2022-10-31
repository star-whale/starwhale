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

import ai.starwhale.mlops.api.DataStoreController;
import ai.starwhale.mlops.api.protocol.datastore.RecordDesc;
import ai.starwhale.mlops.api.protocol.datastore.RecordValueDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.ColumnTypeScalar;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
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

    final DataStoreController dataStore;

    final ObjectMapper objectMapper;

    public IndexWriter(DataStoreController dataStore,
            ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
    }

    public void writeToStore(String tableName, InputStream jsonLine) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(jsonLine))) {
            List<RecordDesc> records = new LinkedList<>();
            String strLine;
            Map<String, ColumnType> tableSchemaMap = null;
            while ((strLine = br.readLine()) != null) {
                if (null == tableSchemaMap) {
                    tableSchemaMap = inferenceTableSchema(strLine);
                }
                Map<String, Object> indexItem = objectMapper.readValue(strLine, Map.class);
                records.add(toRecordDesc(indexItem, tableSchemaMap));
            }
            UpdateTableRequest request = new UpdateTableRequest();
            request.setTableName(tableName);

            request.setTableSchemaDesc(toSchema(tableSchemaMap));
            request.setRecords(records);
            dataStore.updateTable(request);
        } catch (IOException e) {
            log.error("error while reading _meta.jsonl");
            throw new SwProcessException(ErrorType.NETWORK).tip("error while reading _meta.jsonl");
        } finally {
            try {
                jsonLine.close();
            } catch (IOException e) {
                log.error("error closing inputstream for _meta.jsonl");
            }
        }


    }

    private TableSchemaDesc toSchema(Map<String, ColumnType> tableSchemaMap) {
        List<ColumnSchemaDesc> columnSchemaDescs = tableSchemaMap.entrySet().stream()
                .map(entry -> entry.getValue().toColumnSchemaDesc(entry.getKey()))
                .collect(Collectors.toList());
        return new TableSchemaDesc("id", columnSchemaDescs);
    }

    private RecordDesc toRecordDesc(Map<String, Object> indexItem, Map<String, ColumnType> tableSchemaMap) {
        List<RecordValueDesc> values =
                indexItem.entrySet().stream().map(entry -> {
                    String k = entry.getKey();
                    Object v = entry.getValue();
                    return new RecordValueDesc(k, tableSchemaMap.get(k).encode(v, false));
                }).collect(Collectors.toList());
        return new RecordDesc(values);
    }

    private Map<String, ColumnType> inferenceTableSchema(String strLine) throws JsonProcessingException {
        Map<String, Object> row = objectMapper.readValue(strLine, Map.class);
        Map<String, ColumnType> ret = new HashMap<>();
        row.entrySet().forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Long || value instanceof Integer || value instanceof Byte) {
                ret.put(key, ColumnTypeScalar.INT64);
            } else if (value instanceof String) {
                ret.put(key, ColumnTypeScalar.STRING);
            } else if (value instanceof Float || value instanceof Double) {
                ret.put(key, ColumnTypeScalar.FLOAT64);
            } else {
                ret.put(key, ColumnTypeScalar.UNKNOWN);
            }
        });
        return ret;
    }

}
