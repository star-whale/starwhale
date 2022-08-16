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
package ai.starwhale.mlops.memory;

import java.nio.ByteBuffer;

public interface SwBuffer {
    byte getByte(int index);

    void setByte(int index, byte value);

    short getShort(int index);

    void setShort(int index, short value);

    int getInt(int index);

    void setInt(int index, int value);

    long getLong(int index);

    void setLong(int index, long value);

    float getFloat(int index);

    void setFloat(int index, float value);

    double getDouble(int index);

    void setDouble(int index, double value);

    String getString(int index, int count);

    void setString(int index, String value);

    byte[] getBytes(int index, int count);

    void setBytes(int index, byte[] value);

    int capacity();

    void copyTo(SwBuffer buf);

    ByteBuffer asByteBuffer();
}
