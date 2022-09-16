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

import ai.starwhale.mlops.memory.SwBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

public class SwByteBuffer implements SwBuffer {

    private final ByteBuffer buf;

    protected SwByteBuffer(int capacity) {
        this.buf = ByteBuffer.allocate(capacity);
    }

    private SwByteBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public byte getByte(int index) {
        return this.buf.get(index);
    }

    @Override
    public void setByte(int index, byte value) {
        this.buf.put(index, value);
    }

    @Override
    public short getShort(int index) {
        return this.buf.getShort(index);
    }

    @Override
    public void setShort(int index, short value) {
        this.buf.putShort(index, value);
    }

    @Override
    public int getInt(int index) {
        return this.buf.getInt(index);
    }

    @Override
    public void setInt(int index, int value) {
        this.buf.putInt(index, value);
    }

    @Override
    public long getLong(int index) {
        return this.buf.getLong(index);
    }

    @Override
    public void setLong(int index, long value) {
        this.buf.putLong(index, value);
    }

    @Override
    public float getFloat(int index) {
        return this.buf.getFloat(index);
    }

    @Override
    public void setFloat(int index, float value) {
        this.buf.putFloat(index, value);
    }

    @Override
    public double getDouble(int index) {
        return this.buf.getDouble(index);
    }

    @Override
    public void setDouble(int index, double value) {
        this.buf.putDouble(index, value);
    }

    @Override
    public String getString(int index, int count) {
        var b = new byte[count];
        if (this.getBytes(index, b, 0, count) != count) {
            throw new IllegalArgumentException(
                    MessageFormat.format("not enough data. index={0} count={1}", index, count));
        }
        return new String(b, StandardCharsets.UTF_8);
    }

    @Override
    public void setString(int index, String value) {
        var b = value.getBytes(StandardCharsets.UTF_8);
        this.setBytes(index, b, 0, b.length);
    }

    @Override
    public int getBytes(int index, byte[] b, int offset, int len) {
        this.buf.position(index);
        if (len > this.buf.remaining()) {
            len = this.buf.remaining();
        }
        this.buf.get(b, offset, len);
        return len;
    }

    @Override
    public void setBytes(int index, byte[] b, int offset, int len) {
        this.buf.position(index);
        if (len > this.buf.remaining()) {
            len = this.buf.remaining();
        }
        this.buf.put(b, offset, len);
    }

    @Override
    public int capacity() {
        return this.buf.capacity();
    }

    @Override
    public SwBuffer slice(int offset, int len) {
        this.buf.position(offset);
        this.buf.limit(offset + len);
        var buf = new SwByteBuffer(this.buf.slice());
        this.buf.limit(this.buf.capacity());
        return buf;
    }

    @Override
    public void copyTo(SwBuffer buf) {
        buf.setBytes(0, this.buf.array(), this.buf.arrayOffset(), this.buf.limit());
    }

    @Override
    public ByteBuffer asByteBuffer() {
        var buf = this.buf.duplicate();
        buf.clear();
        return buf;
    }
}
