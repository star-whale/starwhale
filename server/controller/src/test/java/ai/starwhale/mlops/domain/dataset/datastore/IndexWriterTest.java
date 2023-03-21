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

package ai.starwhale.mlops.domain.dataset.datastore;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.domain.dataset.index.datastore.IndexWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class IndexWriterTest {

    @Test
    public void testLegacySchema() {
        var dataStore = mock(DataStore.class);
        IndexWriter indexWriter = new IndexWriter(dataStore, new ObjectMapper());
        String meta = "{\"id\":2,\"score\":2.0,\"data_uri\":\"3db33b\",\"data_format\":\"swds_bin\","
                + "\"data_offset\":8128,\"data_size\":4064,\"data_origin\":\"+\","
                + "\"object_store_type\":\"local\",\"data_mime_type\":\"x/undefined\","
                + "\"label\":\"\\u0001\",\"auth_name\":\"\"}\n"
                + "{\"id\":3,\"score\":3.0,\"data_uri\":\"a6d17c9d3\",\"data_format\":\"swds_bin\","
                + "\"data_offset\":12192,\"data_size\":4064,\"data_origin\":\"+\","
                + "\"object_store_type\":\"local\",\"data_mime_type\":\"x/undefined\","
                + "\"label\":\"\\u0000\",\"auth_name\":\"\"}\n"
                + "{\"id\":4,\"score\":4.0,\"data_uri\":\"3db33b80c0929dedc9ffc8773d350eade23534f\","
                + "\"data_format\":\"swds_bin\",\"data_offset\":16256,\"data_size\":4064,"
                + "\"data_origin\":\"+\",\"object_store_type\":\"local\",\"data_mime_type\":\"x/undefined\","
                + "\"label\":\"\\u0004\",\"auth_name\":\"\"}\n"
                + "{\"id\":5,\"score\":5.0,\"data_uri\":\"3db33\",\"data_format\":\"swds_bin\","
                + "\"data_offset\":20320,\"data_size\":4064,\"data_origin\":\"+\","
                + "\"object_store_type\":\"local\",\"data_mime_type\":\"x/undefined\","
                + "\"label\":\"\\u0001\",\"auth_name\":\"\"}";
        indexWriter.writeToStore("table-x", new ByteArrayInputStream(meta.getBytes()));
        var tableSchemaDesc = ArgumentCaptor.forClass(TableSchemaDesc.class);
        var records = ArgumentCaptor.forClass(List.class);
        verify(dataStore).update(eq("table-x"), tableSchemaDesc.capture(), records.capture());
        Assertions.assertEquals(4, records.getValue().size());
        Assertions.assertEquals("id", tableSchemaDesc.getValue().getKeyColumn());
        List<ColumnSchemaDesc> columnSchemaList = tableSchemaDesc.getValue().getColumnSchemaList();
        columnSchemaList.forEach(columnSchemaDesc -> {
            if ("id".equals(columnSchemaDesc.getName())) {
                Assertions.assertEquals(ColumnType.INT64.toString(), columnSchemaDesc.getType());
            }
            if ("data_offset".equals(columnSchemaDesc.getName())) {
                Assertions.assertEquals(ColumnType.INT64.toString(), columnSchemaDesc.getType());
            }
            if ("data_size".equals(columnSchemaDesc.getName())) {
                Assertions.assertEquals(ColumnType.INT64.toString(), columnSchemaDesc.getType());
            }
            if ("data_format".equals(columnSchemaDesc.getName())) {
                Assertions.assertEquals(ColumnType.STRING.toString(), columnSchemaDesc.getType());
            }
            if ("object_store_type".equals(columnSchemaDesc.getName())) {
                Assertions.assertEquals(ColumnType.STRING.toString(), columnSchemaDesc.getType());
            }
            if ("score".equals(columnSchemaDesc.getName())) {
                Assertions.assertEquals(ColumnType.FLOAT64.toString(), columnSchemaDesc.getType());
            }
        });
    }
}
