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
import java.util.Map;
import org.junit.jupiter.api.Test;

class ObjectValueTest {

    @Test
    void testGenerateColumnSchemaDesc() {
        // empty
        var val = new ObjectValue("foo");
        var desc = val.generateColumnSchemaDesc();
        var expected = ColumnSchemaDesc.builder().type("OBJECT").pythonType("foo").attributes(List.of()).build();
        assertEquals(expected, desc.build());

        // non-empty
        val = ObjectValue.valueOf("bar", Map.of("a", new Int8Value((byte) 1), "b", new Int32Value(2)));
        desc = val.generateColumnSchemaDesc();
        expected = ColumnSchemaDesc.builder()
                .type("OBJECT")
                .pythonType("bar")
                .attributes(List.of(
                        ColumnSchemaDesc.int8().name("a").build(),
                        ColumnSchemaDesc.int32().name("b").build()))
                .build();
        assertEquals(expected, desc.build());
    }
}
