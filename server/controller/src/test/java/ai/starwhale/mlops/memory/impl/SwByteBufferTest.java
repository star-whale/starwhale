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

package ai.starwhale.mlops.memory.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SwByteBufferTest {

    private SwByteBuffer buffer;

    @BeforeEach
    public void setUp() {
        this.buffer = new SwByteBuffer(1024);
    }

    @Test
    public void testCapacity() {
        assertThat("", this.buffer.asByteBuffer().capacity(), is(1024));
    }

    @Test
    public void testGetAndSet() {
        this.buffer.setByte(0, (byte) 1);
        assertThat(this.buffer.getByte(0), is((byte) 1));

        this.buffer.setShort(1, (short) 2);
        assertThat(this.buffer.getShort(1), is((short) 2));

        this.buffer.setInt(3, 3);
        assertThat(this.buffer.getInt(3), is(3));

        this.buffer.setLong(7, 4L);
        assertThat(this.buffer.getLong(7), is(4L));

        this.buffer.setFloat(15, 5.f);
        assertThat(this.buffer.getFloat(15), is(5.f));

        this.buffer.setDouble(19, 6.);
        assertThat(this.buffer.getDouble(19), is(6.));

        this.buffer.setString(27, "test");
        assertThat(this.buffer.getString(27, 4), is("test"));

        this.buffer.setBytes(29, "test".getBytes(StandardCharsets.UTF_8), 0, 4);
        byte[] b = new byte[10];
        this.buffer.getBytes(29, b, 1, 4);
        assertThat(Arrays.copyOfRange(b, 1, 5), is("test".getBytes(StandardCharsets.UTF_8)));

        this.buffer.getBytes(0, b, 0, 7);
        assertThat(Arrays.copyOfRange(b, 0, 7), is(new byte[]{1, 0, 2, 0, 0, 0, 3}));
    }

    @Test
    public void testSlice() {
        this.buffer.setString(0, "012345");
        var buf = this.buffer.slice(1, 5);
        assertThat(buf.asByteBuffer(), is(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8))));
        this.buffer.setString(0, "1234567890");
        assertThat(buf.asByteBuffer(), is(ByteBuffer.wrap("23456".getBytes(StandardCharsets.UTF_8))));
    }
}
