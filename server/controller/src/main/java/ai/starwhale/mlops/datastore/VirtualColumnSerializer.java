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

import ai.starwhale.mlops.api.protocol.datastore.RecordListVo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class VirtualColumnSerializer extends StdSerializer<RecordListVo> {

    public VirtualColumnSerializer() {
        this(null);
    }

    public VirtualColumnSerializer(Class<RecordListVo> t) {
        super(t);
    }

    @Override
    public void serialize(RecordListVo value,
                          JsonGenerator jgen,
                          SerializerProvider serializerProvider) throws IOException {
        jgen.writeStartObject();
        var virtualCols = value.getColumnTypes().stream()
                .filter(desc -> ColumnTypeVirtual.TYPE_NAME.equals(desc.getType()))
                .collect(Collectors.toMap(
                    ColumnSchemaDesc::getName, ColumnSchemaDesc::getOrigin
                ));
        if (!virtualCols.isEmpty()) {
            var records = value.getRecords();
            records.forEach(record -> virtualCols.forEach((colAlias, path) -> {
                var newProperty = this.search(path, record);
                // update record
                record.put(colAlias, newProperty);
            }));
        }
        jgen.writeObjectField("columnTypes", value.getColumnTypes());
        jgen.writeObjectField("records", value.getRecords());
        jgen.writeObjectField("lastKey", value.getLastKey());
        jgen.writeEndObject();
    }

    private Object search(String path, Object value) {
        var index = path.indexOf(".");
        if (index == -1) {
            // last
            if (value instanceof Map) {
                return ((Map<?, ?>) value).get(path);
            } else {
                // TODO support list or map<obj, ?>
                throw new UnsupportedOperationException("not support type");
            }
        } else {
            var subPath = path.substring(index + 1);
            var property = path.substring(0, index);
            if (value instanceof Map) {
                var subValue = ((Map<?, ?>) value).get(property);
                return Objects.isNull(subValue) ? null : this.search(subPath, subValue);
            } else {
                // TODO support list or map<obj, ?>
                throw new UnsupportedOperationException("not support type");
            }
        }
    }
}
