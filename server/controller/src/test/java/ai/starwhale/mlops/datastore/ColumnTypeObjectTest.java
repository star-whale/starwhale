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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.SwValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ColumnTypeObjectTest {

    private ColumnTypeObject columnTypeObject;

    @BeforeEach
    public void setUp() {
        this.columnTypeObject = new ColumnTypeObject("test",
                Map.of("a", ColumnTypeScalar.INT32, "b", new ColumnTypeList(ColumnTypeScalar.INT32)));
    }

    @Test
    public void testConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new ColumnTypeObject("", Map.of("a.b", ColumnTypeScalar.INT32)));
    }

    @Test
    public void testGetTypeName() {
        assertThat(this.columnTypeObject.getTypeName(), is("OBJECT"));
    }

    @Test
    public void testToColumnSchemaDesc() {
        var columnSchemaDesc = this.columnTypeObject.toColumnSchemaDesc("t");
        assertThat(columnSchemaDesc.getName(), is("t"));
        assertThat(columnSchemaDesc.getType(), is("OBJECT"));
        assertThat(columnSchemaDesc.getPythonType(), is("test"));
        assertThat(columnSchemaDesc.getElementType(), nullValue());
        assertThat(columnSchemaDesc.getAttributes(), containsInAnyOrder(
                ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                ColumnSchemaDesc.builder()
                        .name("b")
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build()));
    }

    @Test
    public void testToString() {
        assertThat(this.columnTypeObject.toString(),
                anyOf(is("test{a:INT32,b:[INT32]}"), is("test{b:[INT32],a:INT32}")));
    }

    @Test
    public void testIsComparableWith() {
        assertThat(this.columnTypeObject.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(this.columnTypeObject.isComparableWith(ColumnTypeScalar.INT32), is(false));
        assertThat(this.columnTypeObject.isComparableWith(
                        new ColumnTypeObject("test",
                                Map.of("a", ColumnTypeScalar.INT32, "b", new ColumnTypeList(ColumnTypeScalar.INT32)))),
                is(true));
        assertThat(this.columnTypeObject.isComparableWith(
                        new ColumnTypeObject("test",
                                Map.of("a", ColumnTypeScalar.INT32, "b", new ColumnTypeList(ColumnTypeScalar.INT64)))),
                is(false));
    }

    @Test
    public void testEncode() {
        assertThat(this.columnTypeObject.encode(null, false), nullValue());
        assertThat(this.columnTypeObject.encode(null, true), nullValue());
        assertThat(this.columnTypeObject.encode(Map.of("a", 8, "b", List.of(9, 10, 11)), false),
                is(Map.of("a", "00000008", "b", List.of("00000009", "0000000a", "0000000b"))));
        assertThat(this.columnTypeObject.encode(new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                }, false),
                is(new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                }));
        assertThat(this.columnTypeObject.encode(Map.of("a", 8, "b", List.of(9, 10, 11)), true),
                is(Map.of("a", "8", "b", List.of("9", "10", "11"))));
    }

    @Test
    public void testDecode() {
        assertThat(this.columnTypeObject.decode(null), nullValue());
        assertThat(this.columnTypeObject.decode(Map.of("a", "8", "b", List.of("9", "a", "b"))),
                is(Map.of("a", 8, "b", List.of(9, 10, 11))));
        assertThat(this.columnTypeObject.decode(Map.of("a", "8")),
                is(Map.of("a", 8)));
        assertThat(this.columnTypeObject.decode(new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                }),
                is(new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                }));
        assertThrows(SwValidationException.class, () -> this.columnTypeObject.decode("9"));
        assertThrows(SwValidationException.class, () -> this.columnTypeObject.decode(Map.of("c", "8")));
        assertThrows(SwValidationException.class, () -> this.columnTypeObject.decode(Map.of("a", "z")));
    }

    @Test
    public void testNewWalColumnSchema() {
        var schema = this.columnTypeObject.newWalColumnSchema(1, "t").build();
        assertThat(schema.getColumnIndex(), is(1));
        assertThat(schema.getColumnName(), is("t"));
        assertThat(schema.getColumnType(), is("OBJECT"));
        assertThat(schema.getPythonType(), is("test"));
        System.out.println(schema.getAttributesList());
        assertThat(schema.getAttributesList(), containsInAnyOrder(
                Wal.ColumnSchema.newBuilder()
                        .setColumnName("a")
                        .setColumnType("INT32")
                        .build(),
                Wal.ColumnSchema.newBuilder()
                        .setColumnName("b")
                        .setColumnType("LIST")
                        .setElementType(Wal.ColumnSchema.newBuilder().setColumnType("INT32"))
                        .build()));
    }

    @Test
    public void testFromAndToWal() {
        assertThat(this.columnTypeObject.toWal(-1, Map.of("a", 8, "b", List.of(9, 10, 11))).getIndex(), is(-1));
        assertThat(this.columnTypeObject.toWal(10, Map.of("a", 8, "b", List.of(9, 10, 11))).getIndex(), is(10));
        assertThat(this.columnTypeObject.fromWal(this.columnTypeObject.toWal(0, null).build()), nullValue());

        assertThat(this.columnTypeObject.fromWal(
                        this.columnTypeObject.toWal(0, Map.of("a", 8, "b", List.of(9, 10, 11))).build()),
                is(Map.of("a", 8, "b", List.of(9, 10, 11))));
        var nullMap = new HashMap<String, Integer>();
        nullMap.put("a", null);
        nullMap.put("b", null);
        assertThat(this.columnTypeObject.fromWal(this.columnTypeObject.toWal(0, nullMap).build()), is(nullMap));
    }

}
