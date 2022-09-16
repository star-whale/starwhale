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

import java.io.InputStream;
import lombok.Getter;

public class SwBufferInputStream extends InputStream {

    private final SwBuffer buffer;
    @Getter
    private int offset;

    public SwBufferInputStream(SwBuffer buffer) {
        this.buffer = buffer;
    }

    public int remaining() {
        return this.buffer.capacity() - this.offset;
    }

    @Override
    public int read() {
        if (this.offset >= this.buffer.capacity()) {
            return -1;
        }
        return (this.buffer.getByte(this.offset++) & 0xFF);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        var capacity = this.buffer.capacity();
        if (this.offset >= capacity) {
            return -1;
        }
        var ret = this.buffer.getBytes(this.offset, b, off, len);
        this.offset += ret;
        return ret;
    }
}
