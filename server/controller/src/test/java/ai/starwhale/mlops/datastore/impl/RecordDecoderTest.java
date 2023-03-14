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

package ai.starwhale.mlops.datastore.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.ObjectValue;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RecordDecoderTest {

    @Test
    public void testDecodeScalarNoType() {
        assertThat(RecordDecoder.decodeScalar(ColumnType.BOOL, null), nullValue());

        assertThat(RecordDecoder.decodeScalar(ColumnType.BOOL, "0"), is(BaseValue.valueOf(Boolean.FALSE)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.BOOL, "1"), is(BaseValue.valueOf(true)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT8, "a"), is(BaseValue.valueOf((byte) 10)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT8, "ff"), is(BaseValue.valueOf((byte) -1)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT16, "a"), is(BaseValue.valueOf((short) 10)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT16, "ffff"), is(BaseValue.valueOf((short) -1)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT32, "a"), is(BaseValue.valueOf(10)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT32, "ffffffff"), is(BaseValue.valueOf(-1)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT64, "a"), is(BaseValue.valueOf(10L)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT64, "ffffffffffffffff"), is(BaseValue.valueOf(-1L)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.INT64, "efffffffffffffff"),
                is(BaseValue.valueOf(-1152921504606846977L)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT32, Integer.toHexString(Float.floatToIntBits(1.003f))),
                is(BaseValue.valueOf(1.003f)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT32, Integer.toHexString(Float.floatToIntBits(Float.NaN))),
                is(BaseValue.valueOf(Float.NaN)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT32,
                        Integer.toHexString(Float.floatToIntBits(Float.POSITIVE_INFINITY))),
                is(BaseValue.valueOf(Float.POSITIVE_INFINITY)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT32,
                        Integer.toHexString(Float.floatToIntBits(Float.NEGATIVE_INFINITY))),
                is(BaseValue.valueOf(Float.NEGATIVE_INFINITY)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT32,
                        Integer.toHexString(Float.floatToIntBits(Float.MAX_VALUE))),
                is(BaseValue.valueOf(Float.MAX_VALUE)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT32,
                        Integer.toHexString(Float.floatToIntBits(Float.MIN_VALUE))),
                is(BaseValue.valueOf(Float.MIN_VALUE)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT64, Long.toHexString(Double.doubleToLongBits(1.003))),
                is(BaseValue.valueOf(1.003)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT64, Long.toHexString(Double.doubleToLongBits(-1.003))),
                is(BaseValue.valueOf(-1.003)));
        assertThat(
                RecordDecoder.decodeScalar(ColumnType.FLOAT64, Long.toHexString(Double.doubleToLongBits(Double.NaN))),
                is(BaseValue.valueOf(Double.NaN)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT64,
                        Long.toHexString(Double.doubleToLongBits(Double.POSITIVE_INFINITY))),
                is(BaseValue.valueOf(Double.POSITIVE_INFINITY)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT64,
                        Long.toHexString(Double.doubleToLongBits(Double.NEGATIVE_INFINITY))),
                is(BaseValue.valueOf(Double.NEGATIVE_INFINITY)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT64,
                        Long.toHexString(Double.doubleToLongBits(Double.MAX_VALUE))),
                is(BaseValue.valueOf(Double.MAX_VALUE)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.FLOAT64,
                        Long.toHexString(Double.doubleToLongBits(Double.MIN_VALUE))),
                is(BaseValue.valueOf(Double.MIN_VALUE)));
        assertThat(RecordDecoder.decodeScalar(ColumnType.STRING, "test"), is(BaseValue.valueOf("test")));
        assertThat(RecordDecoder.decodeScalar(ColumnType.BYTES,
                        Base64.getEncoder().encodeToString("test\n".getBytes(StandardCharsets.UTF_8))),
                is(BaseValue.valueOf(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8)))));

        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.UNKNOWN, "2"),
                "invalid type");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.BOOL, 2), "not string");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.BOOL, "2"),
                "invalid bool");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.INT8, "z"),
                "invalid int8");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.INT16, "z"),
                "invalid int16");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.INT32, "z"),
                "invalid int32");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.INT64, "z"),
                "invalid int64");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.FLOAT32, "z"),
                "invalid float32");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.FLOAT64, "z"),
                "invalid float64");
        assertThrows(Exception.class, () -> RecordDecoder.decodeScalar(ColumnType.BYTES, "."),
                "invalid bytes");
    }

    @Test
    public void testDecodeValueListNoType() {
        assertThat(RecordDecoder.decodeValue(
                        ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build(),
                        List.of("9", "a", "b")),
                is(BaseValue.valueOf(List.of(9, 10, 11))));
        assertThat(
                RecordDecoder.decodeValue(
                        ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build(),
                        new ArrayList<String>() {
                            {
                                add("0");
                                add(null);
                                add("1");
                            }
                        }),
                is(BaseValue.valueOf(new ArrayList<Integer>() {
                    {
                        add(0);
                        add(null);
                        add(1);
                    }
                })));
        assertThat(RecordDecoder.decodeValue(
                        ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder()
                                        .type("OBJECT")
                                        .pythonType("t")
                                        .attributes(List.of(
                                                ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                                ColumnSchemaDesc.builder().name("b").type("INT32").build()))
                                        .build())
                                .build(),
                        List.of(Map.of("a", "9", "b", "a"), Map.of("a", "a", "b", "b"))),
                is(BaseValue.valueOf(List.of(ObjectValue.valueOf("t", Map.of("a", 9, "b", 10)),
                        ObjectValue.valueOf("t", Map.of("a", 10, "b", 11))))));

        assertThrows(Exception.class, () -> RecordDecoder.decodeValue(
                ColumnSchemaDesc.builder()
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build(),
                "9"));
        assertThrows(Exception.class,
                () -> RecordDecoder.decodeValue(
                        ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build(),
                        List.of("z")));
    }

    @Test
    public void testDecodeValueMapNoType() {
        var schema = ColumnSchemaDesc.builder()
                .type("MAP")
                .keyType(ColumnSchemaDesc.builder().type("INT32").build())
                .valueType(ColumnSchemaDesc.builder().type("INT64").build())
                .build();
        assertThat(RecordDecoder.decodeValue(schema, Map.of("1", "2", "3", "4")),
                is(BaseValue.valueOf(Map.of(1, 2L, 3, 4L))));
        assertThat(
                RecordDecoder.decodeValue(schema,
                        new HashMap<String, String>() {
                            {
                                put("1", null);
                            }
                        }),
                is(BaseValue.valueOf(new HashMap<Integer, Long>() {
                    {
                        put(1, null);
                    }
                })));
        assertThrows(Exception.class, () -> RecordDecoder.decodeValue(schema, "9"));
    }

    @Test
    public void testDecodeValueObjectNoType() {
        var schema = ColumnSchemaDesc.builder()
                .type("OBJECT")
                .pythonType("t")
                .attributes(List.of(
                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                        ColumnSchemaDesc.builder()
                                .name("b")
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().name("element").type("INT32").build())
                                .build()))
                .build();
        assertThat(RecordDecoder.decodeValue(schema, Map.of("a", "8", "b", List.of("9", "a", "b"))),
                is(ObjectValue.valueOf("t", Map.of("a", 8, "b", List.of(9, 10, 11)))));
        assertThat(RecordDecoder.decodeValue(schema, Map.of("a", "8")),
                is(ObjectValue.valueOf("t", Map.of("a", 8))));
        assertThat(RecordDecoder.decodeValue(schema, new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                }),
                is(ObjectValue.valueOf("t", new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                })));
        assertThrows(Exception.class, () -> RecordDecoder.decodeValue(schema, "9"));
        assertThrows(Exception.class, () -> RecordDecoder.decodeValue(schema, Map.of("c", "8")));
        assertThrows(Exception.class, () -> RecordDecoder.decodeValue(schema, Map.of("a", "z")));
    }
}
