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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ColumnTypeTest {

    @Test
    public void testFromColumnSchemaDesc() {
        assertThat("scalar", ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder().type("INT32").build()),
                is(ColumnTypeScalar.INT32));
        assertThat("simple list", ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build()),
                is(new ColumnTypeList(ColumnTypeScalar.INT32)));
        assertThat("composite list", ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .build()),
                is(new ColumnTypeList(new ColumnTypeList(ColumnTypeScalar.INT32))));
        assertThat("simple tuple", ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                        .type("TUPLE")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build()),
                is(new ColumnTypeTuple(ColumnTypeScalar.INT32)));
        assertThat("composite tuple", ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                        .type("TUPLE")
                        .elementType(ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .build()),
                is(new ColumnTypeTuple(new ColumnTypeList(ColumnTypeScalar.INT32))));
        assertThat("object", ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                        .type("OBJECT")
                        .pythonType("t")
                        .attributes(List.of(ColumnSchemaDesc.builder()
                                        .name("a")
                                        .type("INT32")
                                        .build(),
                                ColumnSchemaDesc.builder()
                                        .name("b")
                                        .type("OBJECT")
                                        .pythonType("tt")
                                        .attributes(List.of(
                                                ColumnSchemaDesc.builder()
                                                        .name("a")
                                                        .type("INT32")
                                                        .build(),
                                                ColumnSchemaDesc.builder()
                                                        .name("b")
                                                        .type("INT32")
                                                        .build()))
                                        .build()))
                        .build()),
                is(new ColumnTypeObject("t",
                        Map.of("a", ColumnTypeScalar.INT32,
                                "b", new ColumnTypeObject("tt",
                                        Map.of("a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.INT32))))));

        assertThrows(IllegalArgumentException.class,
                () -> ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder().type("INVALID").build()),
                "invalid type");
        assertThrows(IllegalArgumentException.class,
                () -> ColumnType.fromColumnSchemaDesc(ColumnSchemaDesc.builder().type("LIST").build()),
                "null element type");
        assertThrows(IllegalArgumentException.class,
                () -> ColumnType.fromColumnSchemaDesc(
                        ColumnSchemaDesc.builder().type("OBJECT").pythonType("t").build()),
                "null attribute");
        assertThrows(IllegalArgumentException.class,
                () -> ColumnType.fromColumnSchemaDesc(
                        ColumnSchemaDesc.builder().type("OBJECT").pythonType("t").attributes(List.of()).build()),
                "empty attribute");
        assertThrows(IllegalArgumentException.class,
                () -> ColumnType.fromColumnSchemaDesc(
                        ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .attributes(List.of(ColumnSchemaDesc.builder()
                                        .name("a")
                                        .type("INT32")
                                        .build())).build()),
                "null pythonType");
        assertThrows(IllegalArgumentException.class,
                () -> ColumnType.fromColumnSchemaDesc(
                        ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .pythonType("")
                                .attributes(List.of(ColumnSchemaDesc.builder()
                                        .name("a")
                                        .type("INT32")
                                        .build())).build()),
                "empty pythonType");
    }

    @Test
    public void testCompareNull() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32, null, ColumnTypeScalar.INT32, Integer.MIN_VALUE), is(-1));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32, Integer.MIN_VALUE, ColumnTypeScalar.INT32, null), is(1));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32, null, ColumnTypeScalar.INT32, null), is(0));
    }

    @Test
    public void testCompareScalarBool() {
        assertThat(ColumnType.compare(ColumnTypeScalar.BOOL, false, ColumnTypeScalar.BOOL, false), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.BOOL, false, ColumnTypeScalar.BOOL, true), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.BOOL, true, ColumnTypeScalar.BOOL, false), greaterThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.BOOL, true, ColumnTypeScalar.BOOL, true), equalTo(0));
    }

    @Test
    public void testCompareScalarInt8() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT8, (byte) -9, ColumnTypeScalar.INT8, (byte) 9), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT8, (byte) -9, ColumnTypeScalar.INT8, (byte) -9), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT8, (byte) 9, ColumnTypeScalar.INT8, (byte) -9),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarInt16() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT16, (short) -9, ColumnTypeScalar.INT16, (short) 9),
                lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT16, (short) -9, ColumnTypeScalar.INT16, (short) -9),
                equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT16, (short) 9, ColumnTypeScalar.INT16, (short) -9),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarInt32() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32, -9, ColumnTypeScalar.INT32, 9), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32, -9, ColumnTypeScalar.INT32, -9), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32, 9, ColumnTypeScalar.INT32, -9), greaterThan(0));
    }

    @Test
    public void testCompareScalarInt64() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT64, -9L, ColumnTypeScalar.INT64, 9L), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT64, -9L, ColumnTypeScalar.INT64, -9L), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT64, 9L, ColumnTypeScalar.INT64, -9L), greaterThan(0));
    }

    @Test
    public void testCompareScalarFloat32() {
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT32, -1.03f, ColumnTypeScalar.FLOAT32, 1.03f), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT32, -1.03f, ColumnTypeScalar.FLOAT32, -1.03f), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT32, 1.03f, ColumnTypeScalar.FLOAT32, -1.03f),
                greaterThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32, Float.POSITIVE_INFINITY, ColumnTypeScalar.FLOAT32, 1.03f),
                greaterThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32,
                        Float.POSITIVE_INFINITY,
                        ColumnTypeScalar.FLOAT32,
                        Float.POSITIVE_INFINITY),
                equalTo(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32, Float.NEGATIVE_INFINITY, ColumnTypeScalar.FLOAT32, 1.03f),
                lessThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32,
                        Float.NEGATIVE_INFINITY,
                        ColumnTypeScalar.FLOAT32,
                        Float.NEGATIVE_INFINITY),
                equalTo(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32,
                        Float.NaN,
                        ColumnTypeScalar.FLOAT32,
                        Float.POSITIVE_INFINITY),
                greaterThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32,
                        Float.POSITIVE_INFINITY,
                        ColumnTypeScalar.FLOAT32,
                        Float.NaN),
                lessThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT32, Float.NaN, ColumnTypeScalar.FLOAT32, Float.NaN),
                equalTo(0));
    }

    @Test
    public void testCompareScalarFloat64() {
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64, -1.03, ColumnTypeScalar.FLOAT64, 1.03), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64, -1.03, ColumnTypeScalar.FLOAT64, -1.03), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64, 1.03, ColumnTypeScalar.FLOAT64, -1.03),
                greaterThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64, Double.POSITIVE_INFINITY, ColumnTypeScalar.FLOAT64, 1.03),
                greaterThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Double.POSITIVE_INFINITY,
                        ColumnTypeScalar.FLOAT64,
                        Double.POSITIVE_INFINITY),
                equalTo(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64, Double.NEGATIVE_INFINITY, ColumnTypeScalar.FLOAT64, 1.03),
                lessThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Double.NEGATIVE_INFINITY,
                        ColumnTypeScalar.FLOAT64,
                        Double.NEGATIVE_INFINITY),
                equalTo(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Double.NaN,
                        ColumnTypeScalar.FLOAT64,
                        Double.POSITIVE_INFINITY),
                greaterThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Double.POSITIVE_INFINITY,
                        ColumnTypeScalar.FLOAT64,
                        Double.NaN),
                lessThan(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.FLOAT64, Double.NaN, ColumnTypeScalar.FLOAT64, Double.NaN),
                equalTo(0));
    }

    @Test
    public void testCompareScalarString() {
        assertThat(ColumnType.compare(ColumnTypeScalar.STRING, "", ColumnTypeScalar.STRING, "test"), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.STRING, "", ColumnTypeScalar.STRING, ""), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.STRING, "test", ColumnTypeScalar.STRING, ""), greaterThan(0));
    }

    @Test
    public void testCompareScalarBytes() {
        assertThat(ColumnType.compare(ColumnTypeScalar.BYTES,
                        ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8)),
                        ColumnTypeScalar.BYTES,
                        ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))),
                lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.BYTES,
                        ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8)),
                        ColumnTypeScalar.BYTES,
                        ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))),
                equalTo(0));
        assertThat(
                ColumnType.compare(ColumnTypeScalar.BYTES,
                        ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)),
                        ColumnTypeScalar.BYTES,
                        ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarInt() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT8, Byte.MAX_VALUE, ColumnTypeScalar.INT32, 128), lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT8, Byte.MAX_VALUE, ColumnTypeScalar.INT32, 127), equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT8, Byte.MAX_VALUE, ColumnTypeScalar.INT32, 126),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarFloats() {
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT32, 1.03f, ColumnTypeScalar.FLOAT64, 1.03f + 1e-8),
                lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT32, 1.03f, ColumnTypeScalar.FLOAT64, 1.03f + 0.0),
                equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT32, 1.03f, ColumnTypeScalar.FLOAT64, 1.03f - 1e-8),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarIntAndFloat() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32,
                        Integer.MAX_VALUE,
                        ColumnTypeScalar.FLOAT64,
                        Integer.MAX_VALUE + 1.0),
                lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32,
                        Integer.MAX_VALUE,
                        ColumnTypeScalar.FLOAT64,
                        Integer.MAX_VALUE + 0.0),
                equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT32,
                        Integer.MAX_VALUE,
                        ColumnTypeScalar.FLOAT64,
                        Integer.MAX_VALUE - 1.0),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarFloatAndInt() {
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Integer.MAX_VALUE - 1.0,
                        ColumnTypeScalar.INT32,
                        Integer.MAX_VALUE),
                lessThan(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Integer.MAX_VALUE + 0.0,
                        ColumnTypeScalar.INT32,
                        Integer.MAX_VALUE),
                equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        Integer.MAX_VALUE + 1.0,
                        ColumnTypeScalar.INT32,
                        Integer.MAX_VALUE),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarInt64AndFloat() {
        assertThat(ColumnType.compare(ColumnTypeScalar.INT64,
                        (1L << 53),
                        ColumnTypeScalar.FLOAT64,
                        (1L << 53) + 0.0),
                equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.INT64,
                        (1L << 53) + 1,
                        ColumnTypeScalar.FLOAT64,
                        (1L << 53) + 1 + 0.0),
                greaterThan(0));
    }

    @Test
    public void testCompareScalarFloatAndInt64() {
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        (1L << 53) + 0.0,
                        ColumnTypeScalar.INT64,
                        (1L << 53)),
                equalTo(0));
        assertThat(ColumnType.compare(ColumnTypeScalar.FLOAT64,
                        (1L << 53) + 1 + 0.0,
                        ColumnTypeScalar.INT64,
                        (1L << 53) + 1),
                lessThan(0));
    }

    @Test
    public void testCompareList() {
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 4)),
                lessThan(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 3, 0)),
                lessThan(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 3)),
                equalTo(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 2)),
                greaterThan(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of()),
                greaterThan(0));
    }

    @Test
    public void testCompareTuple() {
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 4)),
                lessThan(0));
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 3, 0)),
                lessThan(0));
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 3)),
                equalTo(0));
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 2)),
                greaterThan(0));
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of()),
                greaterThan(0));
    }

    @Test
    public void testCompareListTuple() {
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 4)),
                lessThan(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 4)),
                lessThan(0));
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 3)),
                equalTo(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 3)),
                equalTo(0));
        assertThat(ColumnType.compare(new ColumnTypeTuple(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeList(ColumnTypeScalar.INT8),
                        List.of(1, 2, 2)),
                greaterThan(0));
        assertThat(ColumnType.compare(new ColumnTypeList(ColumnTypeScalar.INT32),
                        List.of(1, 2, 3),
                        new ColumnTypeTuple(ColumnTypeScalar.INT8),
                        List.of(1, 2, 2)),
                greaterThan(0));
    }

    @Test
    public void testCompareObject() {
        var type = new ColumnTypeObject("t",
                Map.of("a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.INT32, "c", ColumnTypeScalar.INT32));
        var map1 = new LinkedHashMap<>() {
            {
                put("c", 3);
                put("b", 2);
                put("a", 1);
            }
        };
        var map2 = new LinkedHashMap<>() {
            {
                put("c", 3);
                put("b", 3);
                put("a", 1);
            }
        };
        assertThat(ColumnType.compare(type, map1, type, map2), lessThan(0));
        map2.put("b", 2);
        assertThat(ColumnType.compare(type, map1, type, map2), equalTo(0));
        map2.put("b", 1);
        assertThat(ColumnType.compare(type, map1, type, map2), greaterThan(0));
    }
}
