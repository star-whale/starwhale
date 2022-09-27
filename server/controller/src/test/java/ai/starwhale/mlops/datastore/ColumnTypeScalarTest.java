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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class ColumnTypeScalarTest {

    @Test
    public void testGetColumnTypeByName() {
        assertThat(ColumnTypeScalar.getColumnTypeByName("INVALID"), nullValue());
        assertThat(ColumnTypeScalar.getColumnTypeByName("unknown"), is(ColumnTypeScalar.UNKNOWN));
        assertThat(ColumnTypeScalar.getColumnTypeByName("UNKNOWN"), is(ColumnTypeScalar.UNKNOWN));
        assertThat(ColumnTypeScalar.getColumnTypeByName("BOOL"), is(ColumnTypeScalar.BOOL));
        assertThat(ColumnTypeScalar.getColumnTypeByName("INT8"), is(ColumnTypeScalar.INT8));
        assertThat(ColumnTypeScalar.getColumnTypeByName("INT16"), is(ColumnTypeScalar.INT16));
        assertThat(ColumnTypeScalar.getColumnTypeByName("INT32"), is(ColumnTypeScalar.INT32));
        assertThat(ColumnTypeScalar.getColumnTypeByName("INT64"), is(ColumnTypeScalar.INT64));
        assertThat(ColumnTypeScalar.getColumnTypeByName("FLOAT32"), is(ColumnTypeScalar.FLOAT32));
        assertThat(ColumnTypeScalar.getColumnTypeByName("FLOAT64"), is(ColumnTypeScalar.FLOAT64));
        assertThat(ColumnTypeScalar.getColumnTypeByName("STRING"), is(ColumnTypeScalar.STRING));
        assertThat(ColumnTypeScalar.getColumnTypeByName("BYTES"), is(ColumnTypeScalar.BYTES));
    }

    @Test
    public void testGetTypeName() {
        assertThat(ColumnTypeScalar.UNKNOWN.getTypeName(), is("UNKNOWN"));
        assertThat(ColumnTypeScalar.BOOL.getTypeName(), is("BOOL"));
        assertThat(ColumnTypeScalar.INT8.getTypeName(), is("INT8"));
        assertThat(ColumnTypeScalar.INT16.getTypeName(), is("INT16"));
        assertThat(ColumnTypeScalar.INT32.getTypeName(), is("INT32"));
        assertThat(ColumnTypeScalar.INT64.getTypeName(), is("INT64"));
        assertThat(ColumnTypeScalar.FLOAT32.getTypeName(), is("FLOAT32"));
        assertThat(ColumnTypeScalar.FLOAT64.getTypeName(), is("FLOAT64"));
        assertThat(ColumnTypeScalar.STRING.getTypeName(), is("STRING"));
        assertThat(ColumnTypeScalar.BYTES.getTypeName(), is("BYTES"));
    }

    @Test
    public void testToColumnSchemaDesc() {
        assertThat(ColumnTypeScalar.INT32.toColumnSchemaDesc("t"),
                is(ColumnSchemaDesc.builder().name("t").type("INT32").build()));
    }

    @Test
    public void testToString() {
        assertThat(ColumnTypeScalar.UNKNOWN.toString(), is("UNKNOWN"));
        assertThat(ColumnTypeScalar.BOOL.toString(), is("BOOL"));
        assertThat(ColumnTypeScalar.INT8.toString(), is("INT8"));
        assertThat(ColumnTypeScalar.INT16.toString(), is("INT16"));
        assertThat(ColumnTypeScalar.INT32.toString(), is("INT32"));
        assertThat(ColumnTypeScalar.INT64.toString(), is("INT64"));
        assertThat(ColumnTypeScalar.FLOAT32.toString(), is("FLOAT32"));
        assertThat(ColumnTypeScalar.FLOAT64.toString(), is("FLOAT64"));
        assertThat(ColumnTypeScalar.STRING.toString(), is("STRING"));
        assertThat(ColumnTypeScalar.BYTES.toString(), is("BYTES"));
    }

    @Test
    public void testIsComparableWith() {
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.BOOL), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.INT8), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.INT16), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.INT32), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.FLOAT32), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.STRING), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(ColumnTypeScalar.BYTES), is(true));
        assertThat(ColumnTypeScalar.UNKNOWN.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(true));

        assertThat(ColumnTypeScalar.BOOL.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.BOOL.isComparableWith(ColumnTypeScalar.BOOL), is(true));
        assertThat(ColumnTypeScalar.BOOL.isComparableWith(ColumnTypeScalar.INT8), is(false));
        assertThat(ColumnTypeScalar.BOOL.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.INT8.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.INT8.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.INT8.isComparableWith(ColumnTypeScalar.INT8), is(true));
        assertThat(ColumnTypeScalar.INT8.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.INT8.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.INT8.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.INT16.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.INT16.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.INT16.isComparableWith(ColumnTypeScalar.INT16), is(true));
        assertThat(ColumnTypeScalar.INT16.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.INT16.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.INT16.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.INT32.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.INT32.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.INT32.isComparableWith(ColumnTypeScalar.INT32), is(true));
        assertThat(ColumnTypeScalar.INT32.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.INT32.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.INT32.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.INT64.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.INT64.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.INT64.isComparableWith(ColumnTypeScalar.INT8), is(true));
        assertThat(ColumnTypeScalar.INT64.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.INT64.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.INT64.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.FLOAT32.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.FLOAT32.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.FLOAT32.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.FLOAT32.isComparableWith(ColumnTypeScalar.FLOAT32), is(true));
        assertThat(ColumnTypeScalar.FLOAT32.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.FLOAT32.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.FLOAT64.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.FLOAT64.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.FLOAT64.isComparableWith(ColumnTypeScalar.INT64), is(true));
        assertThat(ColumnTypeScalar.FLOAT64.isComparableWith(ColumnTypeScalar.FLOAT32), is(true));
        assertThat(ColumnTypeScalar.FLOAT64.isComparableWith(ColumnTypeScalar.FLOAT64), is(true));
        assertThat(ColumnTypeScalar.FLOAT64.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.STRING.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.STRING.isComparableWith(ColumnTypeScalar.STRING), is(true));
        assertThat(ColumnTypeScalar.STRING.isComparableWith(ColumnTypeScalar.BYTES), is(false));
        assertThat(ColumnTypeScalar.STRING.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));

        assertThat(ColumnTypeScalar.BYTES.isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(ColumnTypeScalar.BYTES.isComparableWith(ColumnTypeScalar.STRING), is(false));
        assertThat(ColumnTypeScalar.BYTES.isComparableWith(ColumnTypeScalar.BYTES), is(true));
        assertThat(ColumnTypeScalar.BYTES.isComparableWith(new ColumnTypeList(ColumnTypeScalar.INT32)), is(false));
    }

    @Test
    public void testEncode() {
        assertThat(ColumnTypeScalar.BOOL.encode(false, false), is("0"));
        assertThat(ColumnTypeScalar.BOOL.encode(false, true), is("false"));
        assertThat(ColumnTypeScalar.BOOL.encode(true, false), is("1"));
        assertThat(ColumnTypeScalar.BOOL.encode(true, true), is("true"));
        assertThat(ColumnTypeScalar.INT8.encode((byte) 10, false), is("a"));
        assertThat(ColumnTypeScalar.INT8.encode((byte) 10, true), is("10"));
        assertThat(ColumnTypeScalar.INT16.encode((short) 10, false), is("a"));
        assertThat(ColumnTypeScalar.INT16.encode((short) 10, true), is("10"));
        assertThat(ColumnTypeScalar.INT32.encode(10, false), is("a"));
        assertThat(ColumnTypeScalar.INT32.encode(10, true), is("10"));
        assertThat(ColumnTypeScalar.INT64.encode(10L, false), is("a"));
        assertThat(ColumnTypeScalar.INT64.encode(10L, true), is("10"));
        assertThat(ColumnTypeScalar.FLOAT32.encode(1.003f, false),
                is(Integer.toHexString(Float.floatToIntBits(1.003f))));
        assertThat(ColumnTypeScalar.FLOAT32.encode(1.003f, true), is("1.003"));
        assertThat(ColumnTypeScalar.FLOAT64.encode(1.003, false), is(Long.toHexString(Double.doubleToLongBits(1.003))));
        assertThat(ColumnTypeScalar.FLOAT64.encode(1.003, true), is("1.003"));
        assertThat(ColumnTypeScalar.STRING.encode("test", false), is("test"));
        assertThat(ColumnTypeScalar.STRING.encode("test", true), is("test"));
        assertThat(ColumnTypeScalar.BYTES.encode(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8)), false),
                is(Base64.getEncoder().encodeToString("test\n".getBytes(StandardCharsets.UTF_8))));
        assertThat(ColumnTypeScalar.BYTES.encode(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8)), true),
                is("test\n"));
    }

    @Test
    public void testDecode() {
        assertThat(ColumnTypeScalar.BOOL.decode(null), nullValue());

        assertThat(ColumnTypeScalar.BOOL.decode("0"), is(Boolean.FALSE));
        assertThat(ColumnTypeScalar.BOOL.decode("1"), is(true));
        assertThat(ColumnTypeScalar.INT8.decode("a"), is((byte) 10));
        assertThat(ColumnTypeScalar.INT16.decode("a"), is((short) 10));
        assertThat(ColumnTypeScalar.INT32.decode("a"), is(10));
        assertThat(ColumnTypeScalar.INT64.decode("a"), is(10L));
        assertThat(ColumnTypeScalar.FLOAT32.decode(Integer.toHexString(Float.floatToIntBits(1.003f))), is(1.003f));
        assertThat(ColumnTypeScalar.FLOAT32.decode(Integer.toHexString(Float.floatToIntBits(Float.NaN))),
                is(Float.NaN));
        assertThat(ColumnTypeScalar.FLOAT32.decode(Integer.toHexString(Float.floatToIntBits(Float.POSITIVE_INFINITY))),
                is(Float.POSITIVE_INFINITY));
        assertThat(ColumnTypeScalar.FLOAT32.decode(Integer.toHexString(Float.floatToIntBits(Float.NEGATIVE_INFINITY))),
                is(Float.NEGATIVE_INFINITY));
        assertThat(ColumnTypeScalar.FLOAT32.decode(Integer.toHexString(Float.floatToIntBits(Float.MAX_VALUE))),
                is(Float.MAX_VALUE));
        assertThat(ColumnTypeScalar.FLOAT32.decode(Integer.toHexString(Float.floatToIntBits(Float.MIN_VALUE))),
                is(Float.MIN_VALUE));
        assertThat(ColumnTypeScalar.FLOAT64.decode(Long.toHexString(Double.doubleToLongBits(1.003))), is(1.003));
        assertThat(ColumnTypeScalar.FLOAT64.decode(Long.toHexString(Double.doubleToLongBits(Double.NaN))),
                is(Double.NaN));
        assertThat(ColumnTypeScalar.FLOAT64.decode(Long.toHexString(Double.doubleToLongBits(Double.POSITIVE_INFINITY))),
                is(Double.POSITIVE_INFINITY));
        assertThat(ColumnTypeScalar.FLOAT64.decode(Long.toHexString(Double.doubleToLongBits(Double.NEGATIVE_INFINITY))),
                is(Double.NEGATIVE_INFINITY));
        assertThat(ColumnTypeScalar.FLOAT64.decode(Long.toHexString(Double.doubleToLongBits(Double.MAX_VALUE))),
                is(Double.MAX_VALUE));
        assertThat(ColumnTypeScalar.FLOAT64.decode(Long.toHexString(Double.doubleToLongBits(Double.MIN_VALUE))),
                is(Double.MIN_VALUE));
        assertThat(ColumnTypeScalar.STRING.decode("test"), is("test"));
        assertThat(ColumnTypeScalar.BYTES.decode(
                        Base64.getEncoder().encodeToString("test\n".getBytes(StandardCharsets.UTF_8))),
                is(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8))));

        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.UNKNOWN.decode("2"), "invalid type");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.BOOL.decode(2), "not string");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.BOOL.decode("2"), "invalid bool");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.INT8.decode("z"), "invalid int8");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.INT16.decode("z"), "invalid int16");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.INT32.decode("z"), "invalid int32");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.INT64.decode("z"), "invalid int64");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.FLOAT32.decode("z"), "invalid float32");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.FLOAT64.decode("z"), "invalid float64");
        assertThrows(SwValidationException.class, () -> ColumnTypeScalar.BYTES.decode("."), "invalid bytes");
    }

    @Test
    public void testFromAndToWal() {
        assertThat(ColumnTypeScalar.INT32.toWal(-1, 9).getIndex(), is(-1));
        assertThat(ColumnTypeScalar.INT32.toWal(10, 9).getIndex(), is(10));
        assertThat(ColumnTypeScalar.INT16.fromWal(ColumnTypeScalar.INT16.toWal(0, null).build()), nullValue());

        assertThat(ColumnTypeScalar.UNKNOWN.fromWal(ColumnTypeScalar.UNKNOWN.toWal(0, null).build()), nullValue());
        assertThat(ColumnTypeScalar.BOOL.fromWal(ColumnTypeScalar.BOOL.toWal(0, false).build()), is(Boolean.FALSE));
        assertThat(ColumnTypeScalar.BOOL.fromWal(ColumnTypeScalar.BOOL.toWal(0, true).build()), is(true));
        assertThat(ColumnTypeScalar.INT8.fromWal(ColumnTypeScalar.INT8.toWal(0, 9).build()), is((byte) 9));
        assertThat(ColumnTypeScalar.INT16.fromWal(ColumnTypeScalar.INT16.toWal(0, 9).build()), is((short) 9));
        assertThat(ColumnTypeScalar.INT32.fromWal(ColumnTypeScalar.INT32.toWal(0, 9).build()), is(9));
        assertThat(ColumnTypeScalar.INT64.fromWal(ColumnTypeScalar.INT64.toWal(0, 9).build()), is(9L));
        assertThat(ColumnTypeScalar.FLOAT32.fromWal(ColumnTypeScalar.FLOAT32.toWal(0, .1f).build()), is(.1f));
        assertThat(ColumnTypeScalar.FLOAT64.fromWal(ColumnTypeScalar.FLOAT64.toWal(0, .1).build()), is(.1));
        assertThat(ColumnTypeScalar.STRING.fromWal(ColumnTypeScalar.STRING.toWal(0, "test").build()), is("test"));
        assertThat(ColumnTypeScalar.BYTES.fromWal(
                        ColumnTypeScalar.BYTES.toWal(0,
                                ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))).build()),
                is(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))));
    }

}
