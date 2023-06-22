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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import ai.starwhale.mlops.datastore.ColumnType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class BaseValueTest {

    @Test
    public void testGetColumnType() {
        assertThat(BaseValue.valueOf(false).getColumnType(), is(ColumnType.BOOL));
        assertThat(BaseValue.valueOf((byte) 0).getColumnType(), is(ColumnType.INT8));
        assertThat(BaseValue.valueOf((short) 0).getColumnType(), is(ColumnType.INT16));
        assertThat(BaseValue.valueOf(0).getColumnType(), is(ColumnType.INT32));
        assertThat(BaseValue.valueOf(0L).getColumnType(), is(ColumnType.INT64));
        assertThat(BaseValue.valueOf(0.f).getColumnType(), is(ColumnType.FLOAT32));
        assertThat(BaseValue.valueOf(0.).getColumnType(), is(ColumnType.FLOAT64));
        assertThat(BaseValue.valueOf("0").getColumnType(), is(ColumnType.STRING));
        assertThat(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])).getColumnType(), is(ColumnType.BYTES));
        assertThat(new ListValue().getColumnType(), is(ColumnType.LIST));
        assertThat(new TupleValue().getColumnType(), is(ColumnType.TUPLE));
        assertThat(new MapValue().getColumnType(), is(ColumnType.MAP));
        assertThat(new ObjectValue("t").getColumnType(), is(ColumnType.OBJECT));
    }

    @Test
    public void testCompareNull() {
        assertThat(BaseValue.compare(null, BaseValue.valueOf(Integer.MIN_VALUE)), is(-1));
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MIN_VALUE), null), is(1));
        assertThat(BaseValue.compare(null, null), is(0));
    }

    @Test
    public void testCompareBool() {
        assertThat(BaseValue.compare(BaseValue.valueOf(false), BaseValue.valueOf(false)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(false), BaseValue.valueOf(true)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(true), BaseValue.valueOf(false)), greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(true), BaseValue.valueOf(true)), equalTo(0));
    }

    @Test
    public void testCompareInt8() {
        assertThat(BaseValue.compare(BaseValue.valueOf((byte) -9), BaseValue.valueOf((byte) 9)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf((byte) -9), BaseValue.valueOf((byte) -9)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf((byte) 9), BaseValue.valueOf((byte) -9)), greaterThan(0));
    }

    @Test
    public void testCompareInt16() {
        assertThat(BaseValue.compare(BaseValue.valueOf((short) -9), BaseValue.valueOf((short) 9)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf((short) -9), BaseValue.valueOf((short) -9)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf((short) 9), BaseValue.valueOf((short) -9)), greaterThan(0));
    }

    @Test
    public void testCompareInt32() {
        assertThat(BaseValue.compare(BaseValue.valueOf(-9), BaseValue.valueOf(9)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(-9), BaseValue.valueOf(-9)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(9), BaseValue.valueOf(-9)), greaterThan(0));
    }

    @Test
    public void testCompareInt64() {
        assertThat(BaseValue.compare(BaseValue.valueOf(-9L), BaseValue.valueOf(9L)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(-9L), BaseValue.valueOf(-9L)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(9L), BaseValue.valueOf(-9L)), greaterThan(0));
    }

    @Test
    public void testCompareFloat32() {
        assertThat(BaseValue.compare(BaseValue.valueOf(-1.03f), BaseValue.valueOf(1.03f)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(-1.03f), BaseValue.valueOf(-1.03f)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(1.03f), BaseValue.valueOf(-1.03f)), greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Float.POSITIVE_INFINITY), BaseValue.valueOf(1.03f)),
                greaterThan(0));
        assertThat(
                BaseValue.compare(BaseValue.valueOf(Float.POSITIVE_INFINITY),
                        BaseValue.valueOf(Float.POSITIVE_INFINITY)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Float.NEGATIVE_INFINITY), BaseValue.valueOf(1.03f)),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Float.NEGATIVE_INFINITY),
                BaseValue.valueOf(Float.NEGATIVE_INFINITY)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Float.NaN), BaseValue.valueOf(Float.POSITIVE_INFINITY)),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Float.POSITIVE_INFINITY), BaseValue.valueOf(Float.NaN)),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Float.NaN), BaseValue.valueOf(Float.NaN)), equalTo(0));
    }

    @Test
    public void testCompareFloat64() {
        assertThat(BaseValue.compare(BaseValue.valueOf(-1.03), BaseValue.valueOf(1.03)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(-1.03), BaseValue.valueOf(-1.03)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(1.03), BaseValue.valueOf(-1.03)), greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.POSITIVE_INFINITY), BaseValue.valueOf(1.03)),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.POSITIVE_INFINITY),
                BaseValue.valueOf(Double.POSITIVE_INFINITY)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.NEGATIVE_INFINITY), BaseValue.valueOf(1.03)),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.NEGATIVE_INFINITY),
                BaseValue.valueOf(Double.NEGATIVE_INFINITY)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.NaN), BaseValue.valueOf(Double.POSITIVE_INFINITY)),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.POSITIVE_INFINITY), BaseValue.valueOf(Double.NaN)),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Double.NaN), BaseValue.valueOf(Double.NaN)), equalTo(0));
    }

    @Test
    public void testCompareString() {
        assertThat(BaseValue.compare(BaseValue.valueOf(""), BaseValue.valueOf("test")), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(""), BaseValue.valueOf("")), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf("test"), BaseValue.valueOf("")), greaterThan(0));
    }

    @Test
    public void testCompareBytes() {
        assertThat(BaseValue.compare(
                        BaseValue.valueOf(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))),
                        BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)))),
                lessThan(0));
        assertThat(BaseValue.compare(
                        BaseValue.valueOf(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))),
                        BaseValue.valueOf(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8)))),
                equalTo(0));
        assertThat(
                BaseValue.compare(
                        BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))),
                        BaseValue.valueOf(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8)))),
                greaterThan(0));
    }

    @Test
    public void testCompareStringBytes() {
        assertThat(BaseValue.compare(BaseValue.valueOf(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))),
                        BaseValue.valueOf("test")),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(""),
                        BaseValue.valueOf(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8)))),
                equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(""), BaseValue.valueOf("")), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))),
                        BaseValue.valueOf("")),
                greaterThan(0));
    }

    @Test
    public void testCompareInt() {
        assertThat(BaseValue.compare(BaseValue.valueOf(Byte.MAX_VALUE), BaseValue.valueOf(128)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Byte.MAX_VALUE), BaseValue.valueOf(127)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Byte.MAX_VALUE), BaseValue.valueOf(126)), greaterThan(0));
    }

    @Test
    public void testCompareFloats() {
        assertThat(BaseValue.compare(BaseValue.valueOf(1.03f), BaseValue.valueOf(1.03f + 1e-8)), lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(1.03f), BaseValue.valueOf(1.03f + 0.0)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(1.03f), BaseValue.valueOf(1.03f - 1e-8)), greaterThan(0));
    }

    @Test
    public void testCompareIntAndFloat() {
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MAX_VALUE), BaseValue.valueOf(Integer.MAX_VALUE + 1.0)),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MAX_VALUE), BaseValue.valueOf(Integer.MAX_VALUE + 0.0)),
                equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MAX_VALUE), BaseValue.valueOf(Integer.MAX_VALUE - 1.0)),
                greaterThan(0));
    }

    @Test
    public void testCompareFloatAndInt() {
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MAX_VALUE - 1.0), BaseValue.valueOf(Integer.MAX_VALUE)),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MAX_VALUE + 0.0), BaseValue.valueOf(Integer.MAX_VALUE)),
                equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Integer.MAX_VALUE + 1.0), BaseValue.valueOf(Integer.MAX_VALUE)),
                greaterThan(0));
    }

    @Test
    public void testCompareInt64AndFloat() {
        assertThat(BaseValue.compare(BaseValue.valueOf((1L << 53)), BaseValue.valueOf((1L << 53) + 0.0)), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf((1L << 53) + 1), BaseValue.valueOf((1L << 53) + 1 + 0.0)),
                greaterThan(0));
    }

    @Test
    public void testCompareFloatAndInt64() {
        assertThat(BaseValue.compare(BaseValue.valueOf((1L << 53) + 0.0), BaseValue.valueOf((1L << 53))), equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf((1L << 53) + 1 + 0.0), BaseValue.valueOf((1L << 53) + 1)),
                lessThan(0));
    }

    @Test
    public void testCompareList() {
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 4))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 3, 0))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 3))),
                equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 2))),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of())),
                greaterThan(0));
        // compare list with null item
        var left = new ArrayList<Integer>() {
            {
                add(1);
                add(2);
                add(null);
            }
        };
        assertThat(BaseValue.compare(BaseValue.valueOf(left), BaseValue.valueOf(List.of(1, 2, 3))), lessThan(0));
        var right = new ArrayList<Integer>() {
            {
                add(1);
                add(2);
                add(null);
            }
        };
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(right)), greaterThan(0));
    }

    @Test
    public void testCompareTuple() {
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 4))),
                lessThan(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 3, 0))),
                lessThan(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 3))),
                equalTo(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 2))),
                greaterThan(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of())),
                greaterThan(0));
    }

    @Test
    public void testCompareListTuple() {
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 4))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 4))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(List.of(1, 2, 3)), TupleValue.valueOf(List.of(1, 2, 3))),
                equalTo(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 3))),
                equalTo(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 2))),
                greaterThan(0));
        assertThat(BaseValue.compare(TupleValue.valueOf(List.of(1, 2, 3)), BaseValue.valueOf(List.of(1, 2, 2))),
                greaterThan(0));
    }

    @Test
    public void testCompareMap() {
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of()),
                        BaseValue.valueOf(Map.of())),
                equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 1)),
                        BaseValue.valueOf(Map.of())),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of()),
                        BaseValue.valueOf(Map.of(1, 1))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 3, 4)),
                        BaseValue.valueOf(Map.of(1, 2, 3, 5))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 3, 4)),
                        BaseValue.valueOf(Map.of(1, 2, 3, 4, 4, 0))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 3, 4)),
                        BaseValue.valueOf(Map.of(1, 2, 2, 0, 3, 4))),
                lessThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 3, 4)),
                        BaseValue.valueOf(Map.of(1, 2, 3, 4))),
                equalTo(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 3, 5)),
                        BaseValue.valueOf(Map.of(1, 2, 3, 4))),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 3, 4, 4, 0)),
                        BaseValue.valueOf(Map.of(1, 2, 3, 4))),
                greaterThan(0));
        assertThat(BaseValue.compare(BaseValue.valueOf(Map.of(1, 2, 2, 0, 3, 4)),
                        BaseValue.valueOf(Map.of(1, 2, 3, 4))),
                greaterThan(0));
    }

    @Test
    public void testCompareObject() {
        var map1 = new ObjectValue("a") {
            {
                put("c", BaseValue.valueOf(3));
                put("b", BaseValue.valueOf(2));
                put("a", BaseValue.valueOf(1));
            }
        };
        var map2 = new ObjectValue("a") {
            {
                put("c", BaseValue.valueOf(3));
                put("b", BaseValue.valueOf(3));
                put("a", BaseValue.valueOf(1));
            }
        };
        var map3 = new ObjectValue("b") {
            {
                put("c", BaseValue.valueOf(3));
                put("b", BaseValue.valueOf(2));
                put("a", BaseValue.valueOf(1));
            }
        };
        assertThat(BaseValue.compare(map1, map2), lessThan(0));
        map2.put("b", BaseValue.valueOf(2));
        assertThat(BaseValue.compare(map1, map2), equalTo(0));
        map2.put("b", BaseValue.valueOf(1));
        assertThat(BaseValue.compare(map1, map2), greaterThan(0));
        assertThat(BaseValue.compare(map1, map3), lessThan(0));
    }

    @Test
    public void compareDifferentTypes() {
        var values = List.of(
                BaseValue.valueOf(false),
                BaseValue.valueOf(0),
                BaseValue.valueOf("0"),
                BaseValue.valueOf(List.of()),
                BaseValue.valueOf(Map.of()),
                new ObjectValue("a"));
        for (int i = 0; i < values.size(); ++i) {
            for (int j = 0; j < values.size(); ++j) {
                var v1 = values.get(i);
                var v2 = values.get(j);
                Matcher<Integer> m;
                if (i < j) {
                    m = lessThan(0);
                } else if (i == j) {
                    m = equalTo(0);
                } else {
                    m = greaterThan(0);
                }
                assertThat(v1.getColumnType() + " " + v2.getColumnType(), BaseValue.compare(v1, v2), m);
            }
        }
    }

    @Test
    public void testEncodeValueScalarNoType() {
        assertThat(BaseValue.encode(null, false, false), nullValue());
        assertThat(BaseValue.encode(BaseValue.valueOf(false), false, false), is("0"));
        assertThat(BaseValue.encode(BaseValue.valueOf(false), true, false), is("false"));
        assertThat(BaseValue.encode(BaseValue.valueOf(true), false, false), is("1"));
        assertThat(BaseValue.encode(BaseValue.valueOf(true), true, false), is("true"));
        assertThat(BaseValue.encode(BaseValue.valueOf((byte) 10), false, false), is("0a"));
        assertThat(BaseValue.encode(BaseValue.valueOf((byte) 10), true, false), is("10"));
        assertThat(BaseValue.encode(BaseValue.valueOf((byte) -1), false, false), is("ff"));
        assertThat(BaseValue.encode(BaseValue.valueOf((short) 10), false, false), is("000a"));
        assertThat(BaseValue.encode(BaseValue.valueOf((short) 10), true, false), is("10"));
        assertThat(BaseValue.encode(BaseValue.valueOf((short) -1), false, false), is("ffff"));
        assertThat(BaseValue.encode(BaseValue.valueOf(10), false, false), is("0000000a"));
        assertThat(BaseValue.encode(BaseValue.valueOf(10), true, false), is("10"));
        assertThat(BaseValue.encode(BaseValue.valueOf(-1), false, false), is("ffffffff"));
        assertThat(BaseValue.encode(BaseValue.valueOf(10L), false, false), is("000000000000000a"));
        assertThat(BaseValue.encode(BaseValue.valueOf(10L), true, false), is("10"));
        assertThat(BaseValue.encode(BaseValue.valueOf(-1L), false, false), is("ffffffffffffffff"));
        assertThat(BaseValue.encode(BaseValue.valueOf(-1152921504606846977L), false, false),
                is("efffffffffffffff"));
        assertThat(BaseValue.encode(BaseValue.valueOf(1.003f), false, false),
                is(Integer.toHexString(Float.floatToIntBits(1.003f))));
        assertThat(BaseValue.encode(BaseValue.valueOf(1.003f), true, false), is("1.003"));
        assertThat(BaseValue.encode(BaseValue.valueOf(1.003), false, false),
                is(Long.toHexString(Double.doubleToLongBits(1.003))));
        assertThat(BaseValue.encode(BaseValue.valueOf(-1.003), false, false),
                is(Long.toHexString(Double.doubleToLongBits(-1.003))));
        assertThat(BaseValue.encode(BaseValue.valueOf(1.003), true, false), is("1.003"));
        assertThat(BaseValue.encode(BaseValue.valueOf("test"), false, false), is("test"));
        assertThat(BaseValue.encode(BaseValue.valueOf("test"), true, false), is("test"));
        assertThat(
                BaseValue.encode(BaseValue.valueOf(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8))),
                        false, false),
                is(Base64.getEncoder().encodeToString("test\n".getBytes(StandardCharsets.UTF_8))));
        assertThat(
                BaseValue.encode(BaseValue.valueOf(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8))),
                        true, false),
                is("test\n"));
    }

    @Test
    public void testEncodeValueListNoType() {
        assertThat(BaseValue.encode(BaseValue.valueOf(List.of(9, 10, 11)), false, false),
                is(List.of("00000009", "0000000a", "0000000b")));
        assertThat(BaseValue.encode(BaseValue.valueOf(List.of(9, 10, 11)), true, false),
                is(List.of("9", "10", "11")));
        assertThat(BaseValue.encode(BaseValue.valueOf(new ArrayList<Integer>() {
                    {
                        add(0);
                        add(null);
                        add(1);
                    }
                }), false, false),
                is(new ArrayList<String>() {
                    {
                        add("00000000");
                        add(null);
                        add("00000001");
                    }
                }));
        var composite = BaseValue.valueOf(List.of(ObjectValue.valueOf("t", Map.of("a", 9, "b", 10)),
                ObjectValue.valueOf("t", Map.of("a", 10, "b", 11))));
        assertThat(BaseValue.encode(composite, false, false),
                is(List.of(Map.of("a", "00000009", "b", "0000000a"), Map.of("a", "0000000a", "b", "0000000b"))));
        assertThat(BaseValue.encode(composite, true, false),
                is(List.of(Map.of("a", "9", "b", "10"), Map.of("a", "10", "b", "11"))));
    }

    @Test
    public void testEncodeValueMapNoType() {
        assertThat(BaseValue.encode(BaseValue.valueOf(Map.of(1, 2L, 3, 4L)), false, false),
                is(Map.of("00000001", "0000000000000002", "00000003", "0000000000000004")));
        assertThat(BaseValue.encode(BaseValue.valueOf(new HashMap<Integer, Long>() {
                    {
                        put(1, null);
                    }
                }), false, false),
                is(new HashMap<String, String>() {
                    {
                        put("00000001", null);
                    }
                }));
        assertThat(BaseValue.encode(BaseValue.valueOf(Map.of(1, 2, 3, 4)), true, false),
                is(Map.of("1", "2", "3", "4")));

        var result = BaseValue.encode(BaseValue.valueOf(Map.of("a", 8, "b", List.of(9, 10, 11))), false, true);
        var expected = Map.of(
                "type", "MAP",
                "value", List.of(
                    Map.of(
                        "key", Map.of("type", "STRING", "value", "a"),
                        "value", Map.of("type", "INT32", "value", "00000008")
                    ),
                    Map.of(
                        "key", Map.of("type", "STRING", "value", "b"),
                        "value", Map.of(
                            "type", "LIST",
                            "value", List.of(
                                Map.of("type", "INT32", "value", "00000009"),
                                Map.of("type", "INT32", "value", "0000000a"),
                                Map.of("type", "INT32", "value", "0000000b")
                            )
                        )
                    )
                )
        );
        assertThat(result, is(expected));
    }

    @Test
    public void testEncodeValueObjectNoType() {
        assertThat(BaseValue.encode(ObjectValue.valueOf("t", Map.of("a", 8, "b", List.of(9, 10, 11))),
                        false,
                        false),
                is(Map.of("a", "00000008", "b", List.of("00000009", "0000000a", "0000000b"))));
        assertThat(BaseValue.encode(ObjectValue.valueOf("t", new HashMap<>() {
                    {
                        put("a", null);
                    }
                }), false, false),
                is(new HashMap<String, Object>() {
                    {
                        put("a", null);
                    }
                }));
        assertThat(BaseValue.encode(ObjectValue.valueOf("t", Map.of("a", 8, "b", List.of(9, 10, 11))),
                        true,
                        false),
                is(Map.of("a", "8", "b", List.of("9", "10", "11"))));

    }

    @Test
    public void testEncodeValueHasType() {
        assertThat(BaseValue.encode(null, true, true),
                is(new HashMap<>() {
                    {
                        put("type", "UNKNOWN");
                        put("value", null);
                    }
                }));
        assertThat(BaseValue.encode(BaseValue.valueOf(false), true, true),
                is(Map.of("type", "BOOL", "value", "false")));
        assertThat(BaseValue.encode(BaseValue.valueOf((byte) 0), true, true),
                is(Map.of("type", "INT8", "value", "0")));
        assertThat(BaseValue.encode(BaseValue.valueOf((short) 0), true, true),
                is(Map.of("type", "INT16", "value", "0")));
        assertThat(BaseValue.encode(BaseValue.valueOf(0), true, true),
                is(Map.of("type", "INT32", "value", "0")));
        assertThat(BaseValue.encode(BaseValue.valueOf(0L), true, true),
                is(Map.of("type", "INT64", "value", "0")));
        assertThat(BaseValue.encode(BaseValue.valueOf(0.f), true, true),
                is(Map.of("type", "FLOAT32", "value", "0.0")));
        assertThat(BaseValue.encode(BaseValue.valueOf(0.), true, true),
                is(Map.of("type", "FLOAT64", "value", "0.0")));
        assertThat(BaseValue.encode(BaseValue.valueOf("0"), true, true),
                is(Map.of("type", "STRING", "value", "0")));
        assertThat(BaseValue.encode(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])), true, true),
                is(Map.of("type", "BYTES", "value", "")));
        assertThat(BaseValue.encode(BaseValue.valueOf(List.of(0)), true, true),
                is(Map.of("type", "LIST", "value", List.of(Map.of("type", "INT32", "value", "0")))));
        assertThat(BaseValue.encode(TupleValue.valueOf(List.of(0)), true, true),
                is(Map.of("type", "TUPLE", "value", List.of(Map.of("type", "INT32", "value", "0")))));
        assertThat(BaseValue.encode(BaseValue.valueOf(Map.of("0", 0)), true, true),
                is(Map.of("type", "MAP",
                        "value", List.of(
                                Map.of(
                                        "key", Map.of("type", "STRING", "value", "0"),
                                        "value", Map.of("type", "INT32", "value", "0"))))));
        assertThat(BaseValue.encode(ObjectValue.valueOf("t", Map.of("0", 0)), true, true),
                is(Map.of("type", "OBJECT",
                        "pythonType", "t",
                        "value", Map.of("0", Map.of("type", "INT32", "value", "0")))));
    }

    @Test
    public void testEqualsAndHashCode() {
        assertThat(BaseValue.valueOf(false) == BaseValue.valueOf(false), is(true));
        assertThat(BaseValue.valueOf(true) == BaseValue.valueOf(true), is(true));
        assertThat(BaseValue.valueOf(false) != BaseValue.valueOf(true), is(true));

        assertThat(BaseValue.valueOf((byte) 0).equals(BaseValue.valueOf((byte) 0)), is(true));
        assertThat(BaseValue.valueOf((byte) 0).hashCode(), is(BaseValue.valueOf((byte) 0).hashCode()));
        assertThat(BaseValue.valueOf((byte) 0).equals(BaseValue.valueOf((byte) 1)), is(false));
        assertThat(BaseValue.valueOf((byte) 0).hashCode(), not(is(BaseValue.valueOf((byte) 1).hashCode())));

        assertThat(BaseValue.valueOf((short) 0).equals(BaseValue.valueOf((short) 0)), is(true));
        assertThat(BaseValue.valueOf((short) 0).hashCode(), is(BaseValue.valueOf((short) 0).hashCode()));
        assertThat(BaseValue.valueOf((short) 0).equals(BaseValue.valueOf((short) 1)), is(false));
        assertThat(BaseValue.valueOf((short) 0).hashCode(), not(is(BaseValue.valueOf((short) 1).hashCode())));

        assertThat(BaseValue.valueOf(0).equals(BaseValue.valueOf(0)), is(true));
        assertThat(BaseValue.valueOf(0).hashCode(), is(BaseValue.valueOf(0).hashCode()));
        assertThat(BaseValue.valueOf(0).equals(BaseValue.valueOf(1)), is(false));
        assertThat(BaseValue.valueOf(0).hashCode(), not(is(BaseValue.valueOf(1).hashCode())));

        assertThat(BaseValue.valueOf(0L).equals(BaseValue.valueOf(0L)), is(true));
        assertThat(BaseValue.valueOf(0L).hashCode(), is(BaseValue.valueOf(0L).hashCode()));
        assertThat(BaseValue.valueOf(0L).equals(BaseValue.valueOf(1L)), is(false));
        assertThat(BaseValue.valueOf(0L).hashCode(), not(is(BaseValue.valueOf(1L).hashCode())));

        assertThat(BaseValue.valueOf(0.f).equals(BaseValue.valueOf(0.f)), is(true));
        assertThat(BaseValue.valueOf(0.f).hashCode(), is(BaseValue.valueOf(0.f).hashCode()));
        assertThat(BaseValue.valueOf(0.f).equals(BaseValue.valueOf(1.f)), is(false));
        assertThat(BaseValue.valueOf(0.f).hashCode(), not(is(BaseValue.valueOf(1.f).hashCode())));

        assertThat(BaseValue.valueOf(0.).equals(BaseValue.valueOf(0.)), is(true));
        assertThat(BaseValue.valueOf(0.).hashCode(), is(BaseValue.valueOf(0.).hashCode()));
        assertThat(BaseValue.valueOf(0.).equals(BaseValue.valueOf(1.)), is(false));
        assertThat(BaseValue.valueOf(0.).hashCode(), not(is(BaseValue.valueOf(1.).hashCode())));

        assertThat(BaseValue.valueOf("0").equals(BaseValue.valueOf("0")), is(true));
        assertThat(BaseValue.valueOf("0").hashCode(), is(BaseValue.valueOf("0").hashCode()));
        assertThat(BaseValue.valueOf("0").equals(BaseValue.valueOf("1")), is(false));
        assertThat(BaseValue.valueOf("0").hashCode(), not(is(BaseValue.valueOf("1").hashCode())));

        assertThat(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])).equals(
                        BaseValue.valueOf(ByteBuffer.wrap(new byte[0]))),
                is(true));
        assertThat(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])).hashCode(),
                is(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])).hashCode()));
        assertThat(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])).equals(
                        BaseValue.valueOf(ByteBuffer.wrap(new byte[1]))),
                is(false));
        assertThat(BaseValue.valueOf(ByteBuffer.wrap(new byte[0])).hashCode(),
                not(is(BaseValue.valueOf(ByteBuffer.wrap(new byte[1])).hashCode())));

        assertThat(BaseValue.valueOf(List.of(0)).equals(BaseValue.valueOf(List.of(0))), is(true));
        assertThat(BaseValue.valueOf(List.of(0)).hashCode(), is(BaseValue.valueOf(List.of(0)).hashCode()));
        assertThat(BaseValue.valueOf(List.of(0)).equals(BaseValue.valueOf(List.of(1))), is(false));
        assertThat(BaseValue.valueOf(List.of(0)).hashCode(), not(is(BaseValue.valueOf(List.of(1)).hashCode())));

        assertThat(TupleValue.valueOf(List.of(0)).equals(TupleValue.valueOf(List.of(0))), is(true));
        assertThat(TupleValue.valueOf(List.of(0)).hashCode(), is(TupleValue.valueOf(List.of(0)).hashCode()));
        assertThat(TupleValue.valueOf(List.of(0)).equals(TupleValue.valueOf(List.of(1))), is(false));
        assertThat(TupleValue.valueOf(List.of(0)).hashCode(), not(is(TupleValue.valueOf(List.of(1)).hashCode())));

        assertThat(BaseValue.valueOf(List.of(0)).equals(TupleValue.valueOf(List.of(0))), is(false));

        assertThat(BaseValue.valueOf(Map.of("0", 0)).equals(BaseValue.valueOf(Map.of("0", 0))), is(true));
        assertThat(BaseValue.valueOf(Map.of("0", 0)).hashCode(), is(BaseValue.valueOf(Map.of("0", 0)).hashCode()));
        assertThat(BaseValue.valueOf(Map.of("0", 0)).equals(BaseValue.valueOf(Map.of("1", 0))), is(false));
        assertThat(BaseValue.valueOf(Map.of("0", 0)).hashCode(), not(is(BaseValue.valueOf(Map.of("1", 0)).hashCode())));

        assertThat(ObjectValue.valueOf("t", Map.of("0", 0)).equals(
                        ObjectValue.valueOf("t", Map.of("0", 0))),
                is(true));
        assertThat(ObjectValue.valueOf("t", Map.of("0", 0)).hashCode(),
                is(ObjectValue.valueOf("t", Map.of("0", 0)).hashCode()));
        assertThat(ObjectValue.valueOf("t", Map.of("0", 0)).equals(
                        ObjectValue.valueOf("t", Map.of("1", 0))),
                is(false));
        assertThat(ObjectValue.valueOf("t", Map.of("0", 0)).hashCode(),
                not(is(ObjectValue.valueOf("t", Map.of("1", 0)).hashCode())));
        assertThat(ObjectValue.valueOf("t", Map.of("0", 0)).equals(
                        ObjectValue.valueOf("t1", Map.of("0", 0))),
                is(false));
        assertThat(ObjectValue.valueOf("t", Map.of("0", 0)).hashCode(),
                not(is(ObjectValue.valueOf("t1", Map.of("0", 0)).hashCode())));

        assertThat(BaseValue.valueOf(Map.of("0", 0)).equals(
                        ObjectValue.valueOf("t", Map.of("0", 0))),
                is(false));
    }
}
