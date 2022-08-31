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

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ColumnTypeTest {
    @Test
    public void testEncodeRawResult() {
        assertThat(ColumnType.BOOL.encode(true, true), is("true"));
        assertThat(ColumnType.INT8.encode((byte) 10, true), is("10"));
        assertThat(ColumnType.INT16.encode((short) 10, true), is("10"));
        assertThat(ColumnType.INT32.encode(10, true), is("10"));
        assertThat(ColumnType.INT64.encode(10L, true), is("10"));
        assertThat(ColumnType.FLOAT32.encode(1.003f, true), is("1.003"));
        assertThat(ColumnType.FLOAT64.encode(1.003, true), is("1.003"));
        assertThat(ColumnType.STRING.encode("test", true), is("test"));
        assertThat(ColumnType.BYTES.encode(ByteBuffer.wrap("test\n".getBytes(StandardCharsets.UTF_8)), true),
                is("test\n"));
    }

    @Test
    public void testDecode() {
        assertThat(ColumnType.BOOL.decode(ColumnType.BOOL.encode(true, false)), is(true));
        assertThat(ColumnType.INT8.decode(ColumnType.INT8.encode(1, false)), is((byte) 1));
        assertThat(ColumnType.INT16.decode(ColumnType.INT16.encode(1, false)), is((short) 1));
        assertThat(ColumnType.INT32.decode(ColumnType.INT32.encode(1, false)), is(1));
        assertThat(ColumnType.INT64.decode(ColumnType.INT64.encode(1L, false)), is(1L));
        assertThat(ColumnType.FLOAT32.decode(ColumnType.FLOAT32.encode(1.003f, false)), is(1.003f));
        assertThat(ColumnType.FLOAT64.decode(ColumnType.FLOAT64.encode(1d, false)), is(1d));
        assertThat(ColumnType.STRING.decode(ColumnType.STRING.encode("test", false)), is("test"));
        assertThat(ColumnType.BYTES.decode(
                        ColumnType.BYTES.encode(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)), false)),
                is(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))));
    }

}
