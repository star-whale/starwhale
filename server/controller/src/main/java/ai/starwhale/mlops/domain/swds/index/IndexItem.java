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

package ai.starwhale.mlops.domain.swds.index;

import ai.starwhale.mlops.api.protocol.datastore.RecordDesc;
import ai.starwhale.mlops.api.protocol.datastore.RecordValueDesc;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * holds information of one piece of data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexItem {

    @JsonProperty("id")
    Long id;

    @JsonProperty("data_uri")
    String dataUri;

    @JsonProperty("data_format")
    String dataFormat;

    @JsonProperty("data_offset")
    Long dataOffset;

    @JsonProperty("data_size")
    Long dataSize;

    @JsonProperty("data_origin")
    String dataOrigin;

    @JsonProperty("label")
    String label;

    public static TableSchemaDesc TABLE_SCHEMA = new TableSchemaDesc("id", List.of(
        new ColumnSchemaDesc("id", ColumnType.INT64.name())
        , new ColumnSchemaDesc("data_uri", ColumnType.STRING.name())
        , new ColumnSchemaDesc("data_format", ColumnType.STRING.name())
        , new ColumnSchemaDesc("data_offset", ColumnType.INT64.name())
        , new ColumnSchemaDesc("data_size", ColumnType.INT64.name())
        , new ColumnSchemaDesc("data_origin", ColumnType.STRING.name())
        , new ColumnSchemaDesc("label", ColumnType.STRING.name())
    ));

    public RecordDesc toRecordDesc(){
        List<RecordValueDesc> ret = new LinkedList<>();
        ret.add(new RecordValueDesc("id",ColumnType.INT64.encode(id)));
        ret.add(new RecordValueDesc("data_uri",dataUri));
        ret.add(new RecordValueDesc("data_format",dataFormat));
        ret.add(new RecordValueDesc("data_offset",ColumnType.INT64.encode(dataOffset)));
        ret.add(new RecordValueDesc("data_size",ColumnType.INT64.encode(dataSize)));
        ret.add(new RecordValueDesc("data_origin",dataOrigin));
        ret.add(new RecordValueDesc("label",label));
        return new RecordDesc(ret);
    }

}
