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

package ai.starwhale.mlops.domain.swds.datastore;

import ai.starwhale.mlops.api.DataStoreController;
import ai.starwhale.mlops.api.protocol.datastore.RecordDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.domain.swds.index.IndexItem;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
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

    public void writeToStore(String tableName, InputStream jsonLine){
        try(BufferedReader br = new BufferedReader(new InputStreamReader(jsonLine))){
            List<RecordDesc> records = new LinkedList<>();
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                IndexItem indexItem = objectMapper.readValue(strLine, IndexItem.class);
                records.add(indexItem.toRecordDesc());
            }
            UpdateTableRequest request = new UpdateTableRequest();
            request.setTableName(tableName);
            request.setTableSchemaDesc(IndexItem.TABLE_SCHEMA);
            request.setRecords(records);
            dataStore.updateTable(request);
        } catch (IOException e) {
            log.error("error while reading _meta.jsonl");
            throw new SWProcessException(ErrorType.NETWORK).tip("error while reading _meta.jsonl");
        } finally {
            try {
                jsonLine.close();
            } catch (IOException e) {
                log.error("error closing inputstream for _meta.jsonl");
            }
        }


    }

}
