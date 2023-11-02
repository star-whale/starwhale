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


import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc.KeyValuePairSchema;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ColumnSchemaTest {

    @Test
    void testGetDiffAndUpdate() {
        var spasePairSchemaDesc = new HashMap<Integer, KeyValuePairSchema>();
        var pair = new KeyValuePairSchema(ColumnSchemaDesc.int16().build(), ColumnSchemaDesc.float32().build());
        spasePairSchemaDesc.put(1, pair);
        var baseSchemaDesc = ColumnSchemaDesc.mapOf(ColumnSchemaDesc.int8(), ColumnSchemaDesc.string()).build();
        baseSchemaDesc.setSparseKeyValuePairSchema(spasePairSchemaDesc);

        // generate new schema desc with the different sparse pair of key
        var pairWithDiffValueType =
                new KeyValuePairSchema(ColumnSchemaDesc.int16().build(), ColumnSchemaDesc.float64().build());
        var newSparsePairSchemaDesc = new HashMap<Integer, KeyValuePairSchema>();
        newSparsePairSchemaDesc.put(1, pairWithDiffValueType);
        var newSchemaDesc = ColumnSchemaDesc.mapOf(ColumnSchemaDesc.int8(), ColumnSchemaDesc.string()).build();
        newSchemaDesc.setSparseKeyValuePairSchema(newSparsePairSchemaDesc);

        var colSchema = new ColumnSchema(baseSchemaDesc, 0);
        var diff = colSchema.getDiff(newSchemaDesc);
        var expect = Wal.ColumnSchema.newBuilder()
                .setColumnType("MAP")
                .putSparseKeyValueTypes(1, Wal.ColumnSchema.KeyValuePair.newBuilder()
                        .setValue(Wal.ColumnSchema.newBuilder()
                                .setColumnType("FLOAT64")
                                .build())
                        .build());
        assertEquals(expect.build(), diff.build());


        // update with diff, and check if the schema is updated to the desired one
        colSchema.update(diff.build());
        assertEquals(new ColumnSchema(newSchemaDesc, 0), colSchema);
    }
}
