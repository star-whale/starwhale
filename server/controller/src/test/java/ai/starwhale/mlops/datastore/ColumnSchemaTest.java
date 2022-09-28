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

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.SwValidationException;
import org.junit.jupiter.api.Test;

public class ColumnSchemaTest {

    private ColumnSchema schema;

    @Test
    public void testConstructor() {
        new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0);
        new ColumnSchema(ColumnSchemaDesc.builder()
                .name("k")
                .type("LIST")
                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                .build(),
                0);
    }

    @Test
    public void testConstructorException() {
        assertThrows(NullPointerException.class, () -> new ColumnSchema(null, 0), "null schema");

        assertThrows(SwValidationException.class,
                () -> new ColumnSchema(ColumnSchemaDesc.builder().type("STRING").build(), 0),
                "null column name");

        assertThrows(SwValidationException.class,
                () -> new ColumnSchema(ColumnSchemaDesc.builder().name("k").build(), 0),
                "null type");

        assertThrows(SwValidationException.class,
                () -> new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("invalid").build(), 0),
                "invalid type");
    }
}
