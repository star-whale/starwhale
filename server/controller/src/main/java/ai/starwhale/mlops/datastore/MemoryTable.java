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

import ai.starwhale.mlops.datastore.type.BaseValue;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface MemoryTable {

    String getTableName();

    TableSchema getSchema();

    void updateFromWal(Wal.WalEntry entry);

    // update records, returns the timestamp in milliseconds
    long update(TableSchemaDesc schema, List<Map<String, Object>> records);

    /**
     * update records, returns the timestamp in milliseconds
     *
     * @param schema  table schema, DO NOT RELY ON THE COLUMN SCHEMA FROM THIS PARAMETER
     *                you can rely on the column name, index and the key column name
     * @param records records to update
     * @return timestamp in milliseconds
     */
    long updateWithObject(TableSchemaDesc schema, List<Map<String, BaseValue>> records);

    Iterator<RecordResult> query(
            long timestamp,
            Map<String, String> columns,
            List<OrderByDesc> orderBy,
            TableQueryFilter filter,
            boolean keepNone
    );

    Iterator<RecordResult> scan(
            long timestamp,
            Map<String, String> columns,
            String start,
            String startType,
            boolean startInclusive,
            String end,
            String endType,
            boolean endInclusive,
            boolean keepNone
    );

    void lock(boolean forRead);

    void unlock(boolean forRead);

    void save() throws IOException;

    long getFirstWalLogId();

    long getLastUpdateTime();

    long getLastRevision();

    Map<String, ColumnStatistics> getColumnStatistics(Map<String, String> columnMapping);
}
