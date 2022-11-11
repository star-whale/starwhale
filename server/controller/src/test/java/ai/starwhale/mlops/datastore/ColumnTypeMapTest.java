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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.SwValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ColumnTypeMapTest {

    private ColumnTypeMap columnTypeMap;

    @BeforeEach
    public void setUp() {
        this.columnTypeMap = new ColumnTypeMap(ColumnTypeScalar.INT32, ColumnTypeScalar.INT64);
    }

    @Test
    public void testGetTypeName() {
        assertThat(this.columnTypeMap.getTypeName(), is("MAP"));
    }

    @Test
    public void testToColumnSchemaDesc() {
        assertThat(this.columnTypeMap.toColumnSchemaDesc("t"),
                is(ColumnSchemaDesc.builder()
                        .name("t")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder()
                                .type("INT32")
                                .build())
                        .valueType(ColumnSchemaDesc.builder()
                                .type("INT64")
                                .build())
                        .build()));
    }

    @Test
    public void testToString() {
        assertThat(this.columnTypeMap.toString(), is("{INT32:INT64}"));
    }

    @Test
    public void testIsComparableWith() {
        assertThat(this.columnTypeMap.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(this.columnTypeMap.isComparableWith(ColumnTypeScalar.INT32), is(false));
        assertThat(this.columnTypeMap.isComparableWith(
                        new ColumnTypeMap(ColumnTypeScalar.INT32, ColumnTypeScalar.INT8)),
                is(true));
        assertThat(this.columnTypeMap.isComparableWith(
                        new ColumnTypeMap(ColumnTypeScalar.INT8, ColumnTypeScalar.INT64)),
                is(false));
    }

    @Test
    public void testEncode() {
        assertThat(this.columnTypeMap.encode(null, false), nullValue());
        assertThat(this.columnTypeMap.encode(null, true), nullValue());
        assertThat(this.columnTypeMap.encode(Map.of(1, 2, 3, 4), false),
                is(Map.of("00000001", "0000000000000002", "00000003", "0000000000000004")));
        assertThat(this.columnTypeMap.encode(new HashMap<Integer, Long>() {
                    {
                        put(1, null);
                    }
                }, false),
                is(new HashMap<String, String>() {
                    {
                        put("00000001", null);
                    }
                }));
        assertThat(this.columnTypeMap.encode(Map.of(1, 2, 3, 4), true),
                is(Map.of("1", "2", "3", "4")));
    }

    @Test
    public void testDecode() {
        assertThat(this.columnTypeMap.decode(null), nullValue());
        assertThat(this.columnTypeMap.decode(Map.of("1", "2", "3", "4")), is(Map.of(1, 2L, 3, 4L)));
        assertThat(this.columnTypeMap.decode(new HashMap<String, String>() {
                    {
                        put("1", null);
                    }
                }),
                is(new HashMap<Integer, Long>() {
                    {
                        put(1, null);
                    }
                }));
        assertThrows(SwValidationException.class, () -> this.columnTypeMap.decode("9"));
    }

    @Test
    public void testNewWalColumnSchema() {
        assertThat(this.columnTypeMap.newWalColumnSchema(1, "t").build(),
                is(Wal.ColumnSchema.newBuilder()
                        .setColumnIndex(1)
                        .setColumnName("t")
                        .setColumnType("MAP")
                        .setKeyType(Wal.ColumnSchema.newBuilder().setColumnType("INT32"))
                        .setValueType(Wal.ColumnSchema.newBuilder().setColumnType("INT64"))
                        .build()));
    }

    @Test
    public void testFromAndToWal() {
        assertThat(this.columnTypeMap.toWal(-1, Map.of(1, 2L, 3, 4L)).getIndex(), is(-1));
        assertThat(this.columnTypeMap.toWal(10, Map.of(1, 2L, 3, 4L)).getIndex(), is(10));
        assertThat(this.columnTypeMap.fromWal(this.columnTypeMap.toWal(0, null).build()), nullValue());

        assertThat(this.columnTypeMap.fromWal(
                        this.columnTypeMap.toWal(0, Map.of(1, 2L, 3, 4L)).build()),
                is(Map.of(1, 2L, 3, 4L)));
        var nullMap = new HashMap<Integer, Long>();
        nullMap.put(1, null);
        nullMap.put(2, null);
        assertThat(this.columnTypeMap.fromWal(this.columnTypeMap.toWal(0, nullMap).build()), is(nullMap));
    }

}
