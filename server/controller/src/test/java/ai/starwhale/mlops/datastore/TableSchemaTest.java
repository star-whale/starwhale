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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableSchemaTest {

    private TableSchema schema;

    @BeforeEach
    public void setUp() {
        this.schema = new TableSchema(
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())));
    }

    @Test
    public void testConstructor() {
        new TableSchema(new TableSchemaDesc(
                "k",
                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("123").type("INT8").build(),
                        ColumnSchemaDesc.builder().name("a/b-c/d:e_f").type("INT8").build())));
    }

    @Test
    public void testConstructorException() {
        assertThrows(SwValidationException.class,
                () -> new TableSchema(new TableSchemaDesc(null,
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build()))),
                "null key");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(new TableSchemaDesc("k", null)),
                "null columns");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(new TableSchemaDesc("k", List.of())),
                "empty columns");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(
                        new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("a").type("STRING").build()))),
                "no key");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder().name("k").type("UNKNOWN").build()))),
                "invalid key type unknown");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder()
                                        .name("k")
                                        .type("LIST")
                                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                        .build()))),
                "invalid key type list");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder()
                                        .name("k")
                                        .type("OBJECT")
                                        .pythonType("t")
                                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build()))
                                        .build()))),
                "invalid key type object");

        assertThrows(SwValidationException.class,
                () -> new TableSchema(
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                        ColumnSchemaDesc.builder().name("k").type("STRING").build()))),
                "duplicate key");

    }

    @Test
    public void testCopyConstructor() {
        var newSchema = new TableSchema(this.schema);
        assertThat("same", newSchema, equalTo(this.schema));
        newSchema.merge(new TableSchemaDesc(
                "k",
                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("b").type("FLOAT32").build())));
        assertThat("independent", newSchema, not(equalTo(this.schema)));
    }

    @Test
    public void testGetColumnSchemaByName() {
        assertThat("common",
                this.schema.getColumnSchemaByName("k"),
                is(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0)));
        assertThat("null", this.schema.getColumnSchemaByName("x"), nullValue());
    }

    @Test
    public void testGetColumnSchemas() {
        var columnSchemas = this.schema.getColumnSchemas();
        assertThat("equals", columnSchemas, containsInAnyOrder(
                new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1)));
        assertThrows(UnsupportedOperationException.class, columnSchemas::clear, "read only");
    }

    @Test
    public void testMerge() {
        assertThrows(NullPointerException.class, () -> this.schema.merge(null), "null");

        assertThrows(SwValidationException.class,
                () -> this.schema.merge(
                        new TableSchemaDesc("a", List.of(ColumnSchemaDesc.builder().name("a").type("STRING").build()))),
                "conflicting key");

        assertThrows(SwValidationException.class,
                () -> this.schema.merge(
                        new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build()))),
                "conflicting type 1");

        assertThrows(SwValidationException.class,
                () -> this.schema.merge(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("STRING").build()))),
                "conflicting type 2");

        var diff = this.schema.merge(new TableSchemaDesc("k", List.of(
                ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                ColumnSchemaDesc.builder().name("b").type("FLOAT32").build())));
        assertThat("new column", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2)));
        assertThat("new column", diff,
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2)));

        diff = this.schema.merge(
                new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build())));
        assertThat("new unknown column", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build(), 3)));
        assertThat("new unknown column",
                diff,
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build(), 3)));

        diff = this.schema.merge(
                new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("k").type("UNKNOWN").build())));
        assertThat("merge unknown to existing", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build(), 3)));
        assertThat("merge unknown to existing", diff, empty());

        diff = this.schema.merge(
                new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build())));
        assertThat("merge unknown to unknown", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build(), 3)));
        assertThat("merge unknown to unknown", diff, empty());

        diff = this.schema.merge(
                new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("x").type("INT32").build())));
        assertThat("merge other to unknown", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("INT32").build(), 3)));
        assertThat("merge other to unknown",
                diff,
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("INT32").build(), 3)));

        var listColumnSchema = ColumnSchemaDesc.builder()
                .name("l")
                .type("LIST")
                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                .build();
        this.schema.merge(new TableSchemaDesc(null, List.of(listColumnSchema)));
        diff = this.schema.merge(
                new TableSchemaDesc(null,
                        List.of(ColumnSchemaDesc.builder()
                                .name("l")
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())));
        assertThat("merge list to list", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("INT32").build(), 3),
                        new ColumnSchema(listColumnSchema, 4)));
        assertThat("merge list to list", diff, empty());

        diff = this.schema.merge(new TableSchemaDesc(null,
                List.of(ColumnSchemaDesc.builder().name("y").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("z").type("BYTES").build())));
        assertThat("merge multiple", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("k").type("STRING").build(), 0),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("a").type("INT32").build(), 1),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("b").type("FLOAT32").build(), 2),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("x").type("INT32").build(), 3),
                        new ColumnSchema(listColumnSchema, 4),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("y").type("STRING").build(), 5),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("z").type("BYTES").build(), 6)));
        assertThat("merge multiple",
                diff,
                containsInAnyOrder(new ColumnSchema(ColumnSchemaDesc.builder().name("y").type("STRING").build(), 5),
                        new ColumnSchema(ColumnSchemaDesc.builder().name("z").type("BYTES").build(), 6)));
    }

    @Test
    public void testGetColumnTypeMapping() {
        this.schema.merge(new TableSchemaDesc("k",
                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("b").type("FLOAT32").build())));

        assertThat("all",
                this.schema.getColumnTypeMapping(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.FLOAT32)));

        assertThat("single",
                this.schema.getColumnTypeMapping(Map.of("k", "x")),
                is(Map.of("x", ColumnTypeScalar.STRING)));

        assertThat("multiple",
                this.schema.getColumnTypeMapping(Map.of("k", "x", "a", "a")),
                is(Map.of("x", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32)));

        assertThat("empty",
                this.schema.getColumnTypeMapping(Map.of()),
                is(Map.of()));

        assertThrows(NullPointerException.class,
                () -> this.schema.getColumnTypeMapping(null),
                "null");

        assertThrows(SwValidationException.class,
                () -> this.schema.getColumnTypeMapping(Map.of("x", "x")),
                "extra column");
    }
}
