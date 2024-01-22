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

package ai.starwhale.mlops.datastore.type;


import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListValueTest {
    @Test
    public void testGenerateColumnSchemaDesc() {
        // empty
        var val = new ListValue();
        var desc = val.generateColumnSchemaDesc();
        assertEquals(ColumnSchemaDesc.builder().type("LIST").build(), desc.build());

        // only one item type
        val = new ListValue();
        for (var i = 0; i < 7; i++) {
            val.add(new Int8Value((byte) 1));
        }
        desc = val.generateColumnSchemaDesc();
        var expected = ColumnSchemaDesc.builder()
                .type("LIST")
                .elementType(ColumnSchemaDesc.int8().name("element").build())
                .build();
        assertEquals(expected, desc.build());

        // 3 item types
        val = new ListValue();
        for (var i = 0; i < 7; i++) {
            val.add(new Int8Value((byte) 1));
        }
        val.add(new Int32Value(1));
        val.add(new Float32Value(1.0f));

        desc = val.generateColumnSchemaDesc();
        expected = ColumnSchemaDesc.builder()
                .type("LIST")
                .elementType(ColumnSchemaDesc.int8().name("element").build())
                .attributes(List.of(
                        ColumnSchemaDesc.int32().name("element").index(7).build(),
                        ColumnSchemaDesc.float32().name("element").index(8).build()))
                .build();
        assertEquals(expected, desc.build());
    }
}
