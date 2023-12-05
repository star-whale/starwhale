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

package ai.starwhale.mlops.datastore.impl;

import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.exception.SwValidationException;
import java.text.MessageFormat;
import java.util.Map;
import lombok.NonNull;

public class WalRecordEncoder {

    public static Wal.Record.Builder encodeRecord(@NonNull TableSchema schema, @NonNull Map<String, BaseValue> record) {
        var ret = Wal.Record.newBuilder();
        for (var entry : record.entrySet()) {
            var name = entry.getKey();
            if (name.equals(MemoryTableImpl.DELETED_FLAG_COLUMN_NAME)) {
                ret.addColumns(Wal.Column.newBuilder().setIndex(-1));
                continue;
            }
            try {
                Wal.Column.Builder col;
                if (entry.getValue() == null) {
                    col = Wal.Column.newBuilder().setNullValue(true);
                } else {
                    col = BaseValue.encodeWal(entry.getValue());
                }
                col.setIndex(schema.getColumnIndexByName(name));
                ret.addColumns(col);
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("failed to encode value {0} for column {1}",
                                entry.getValue(),
                                name),
                        e);
            }
        }
        return ret;
    }
}
