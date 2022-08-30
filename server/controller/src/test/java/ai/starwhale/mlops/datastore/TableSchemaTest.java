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

import ai.starwhale.mlops.exception.SWValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TableSchemaTest {

    private TableSchema schema;

    @BeforeEach
    public void setUp() {
        this.schema = new TableSchema(
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "INT32"))));
    }

    @Test
    public void testConstructor() {
        new TableSchema(new TableSchemaDesc(
                "k",
                List.of(new ColumnSchemaDesc("k", "STRING"),
                        new ColumnSchemaDesc("a/b-c/d:e_f", "INT8"))));
    }

    @Test
    public void testConstructorException() {
        assertThrows(SWValidationException.class,
                () -> new TableSchema(new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("k", "STRING")))),
                "null key");

        assertThrows(SWValidationException.class,
                () -> new TableSchema(new TableSchemaDesc("k", null)),
                "null columns");

        assertThrows(SWValidationException.class,
                () -> new TableSchema(new TableSchemaDesc("k", List.of())),
                "empty columns");

        assertThrows(SWValidationException.class,
                () -> new TableSchema(new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("a", "STRING")))),
                "no key");

        assertThrows(SWValidationException.class,
                () -> new TableSchema(new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "UNKNOWN")))),
                "invalid key type");

        assertThrows(SWValidationException.class,
                () -> new TableSchema(
                        new TableSchemaDesc("k",
                                List.of(new ColumnSchemaDesc("k", "STRING"),
                                        new ColumnSchemaDesc("k", "STRING")))),
                "duplicate key");

    }

    @Test
    public void testCopyConstructor() {
        var newSchema = new TableSchema(this.schema);
        assertThat("same", newSchema, equalTo(this.schema));
        newSchema.merge(new TableSchemaDesc(
                "k",
                List.of(new ColumnSchemaDesc("k", "STRING"),
                        new ColumnSchemaDesc("b", "FLOAT32"))));
        assertThat("independent", newSchema, not(equalTo(this.schema)));
    }

    @Test
    public void testGetColumnSchemaByName() {
        assertThat("common",
                this.schema.getColumnSchemaByName("k"),
                is(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0)));
        assertThat("null", this.schema.getColumnSchemaByName("x"), nullValue());
    }

    @Test
    public void testGetColumnSchemas() {
        var columnSchemas = this.schema.getColumnSchemas();
        assertThat("equals", columnSchemas, containsInAnyOrder(
                new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1)));
        assertThrows(UnsupportedOperationException.class, columnSchemas::clear, "read only");
    }

    @Test
    public void testMerge() {
        assertThrows(NullPointerException.class, () -> this.schema.merge(null), "null");

        assertThrows(SWValidationException.class,
                () -> this.schema.merge(new TableSchemaDesc("a", List.of(new ColumnSchemaDesc("a", "STRING")))),
                "conflicting key");

        assertThrows(SWValidationException.class,
                () -> this.schema.merge(new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "INT32")))),
                "conflicting type 1");

        assertThrows(SWValidationException.class,
                () -> this.schema.merge(new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "STRING")))),
                "conflicting type 2");

        var diff = this.schema.merge(new TableSchemaDesc("k", List.of(
                new ColumnSchemaDesc("k", "STRING"),
                new ColumnSchemaDesc("b", "FLOAT32"))));
        assertThat("new column", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                        new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1),
                        new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2)));
        assertThat("new column", diff, containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2)));

        diff = this.schema.merge(new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("x", "UNKNOWN"))));
        assertThat("new unknown column", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                        new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1),
                        new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2),
                        new ColumnSchema(new ColumnSchemaDesc("x", "UNKNOWN"), 3)));
        assertThat("new unknown column",
                diff,
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("x", "UNKNOWN"), 3)));

        diff = this.schema.merge(new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("k", "UNKNOWN"))));
        assertThat("merge unknown to existing", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                        new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1),
                        new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2),
                        new ColumnSchema(new ColumnSchemaDesc("x", "UNKNOWN"), 3)));
        assertThat("merge unknown to existing", diff, empty());

        diff = this.schema.merge(new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("x", "UNKNOWN"))));
        assertThat("merge unknown to unknown", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                        new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1),
                        new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2),
                        new ColumnSchema(new ColumnSchemaDesc("x", "UNKNOWN"), 3)));
        assertThat("merge unknown to unknown", diff, empty());

        diff = this.schema.merge(new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("x", "INT32"))));
        assertThat("merge other to unknown", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                        new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1),
                        new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2),
                        new ColumnSchema(new ColumnSchemaDesc("x", "INT32"), 3)));
        assertThat("merge other to unknown",
                diff,
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("x", "INT32"), 3)));

        diff = this.schema.merge(new TableSchemaDesc(null,
                List.of(new ColumnSchemaDesc("y", "STRING"), new ColumnSchemaDesc("z", "BYTES"))));
        assertThat("merge multiple", this.schema.getColumnSchemas(),
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("k", "STRING"), 0),
                        new ColumnSchema(new ColumnSchemaDesc("a", "INT32"), 1),
                        new ColumnSchema(new ColumnSchemaDesc("b", "FLOAT32"), 2),
                        new ColumnSchema(new ColumnSchemaDesc("x", "INT32"), 3),
                        new ColumnSchema(new ColumnSchemaDesc("y", "STRING"), 4),
                        new ColumnSchema(new ColumnSchemaDesc("z", "BYTES"), 5)));
        assertThat("merge multiple",
                diff,
                containsInAnyOrder(new ColumnSchema(new ColumnSchemaDesc("y", "STRING"), 4),
                        new ColumnSchema(new ColumnSchemaDesc("z", "BYTES"), 5)));
    }

    @Test
    public void testGetColumnTypeMapping() {
        this.schema.merge(new TableSchemaDesc("k",
                List.of(new ColumnSchemaDesc("k", "STRING"),
                        new ColumnSchemaDesc("b", "FLOAT32"))));

        assertThat("all",
                this.schema.getColumnTypeMapping(),
                is(Map.of("k", ColumnType.STRING, "a", ColumnType.INT32, "b", ColumnType.FLOAT32)));

        assertThat("single",
                this.schema.getColumnTypeMapping(Map.of("k", "x")),
                is(Map.of("x", ColumnType.STRING)));

        assertThat("multiple",
                this.schema.getColumnTypeMapping(Map.of("k", "x", "a", "a")),
                is(Map.of("x", ColumnType.STRING, "a", ColumnType.INT32)));

        assertThat("empty",
                this.schema.getColumnTypeMapping(Map.of()),
                is(Map.of()));

        assertThrows(NullPointerException.class,
                () -> this.schema.getColumnTypeMapping(null),
                "null");

        assertThrows(SWValidationException.class,
                () -> this.schema.getColumnTypeMapping(Map.of("x", "x")),
                "extra column");
    }
}
