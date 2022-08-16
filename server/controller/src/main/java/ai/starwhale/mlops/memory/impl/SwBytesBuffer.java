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

public class SwBytesBuffer implements SwBuffer {
    private ByteBuffer buf;

    protected SwBytesBuffer(int capacity) {
        this.buf = ByteBuffer.allocate(capacity);
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
        return new String(this.getBytes(index, count), StandardCharsets.UTF_8);
    }

    @Override
    public void setString(int index, String value) {
        this.setBytes(index, value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] getBytes(int index, int count) {
        byte[] data = new byte[count];
        this.buf.position(index);
        this.buf.get(data);
        return data;
    }

    @Override
    public void setBytes(int index, byte[] value) {
        this.buf.position(index);
        this.buf.put(value);
    }

    @Override
    public int capacity() {
        return this.buf.capacity();
    }

    @Override
    public void copyTo(SwBuffer buf) {
        buf.setBytes(0, this.buf.array());
    }

    @Override
    public ByteBuffer asByteBuffer() {
        return this.buf;
    }
}
