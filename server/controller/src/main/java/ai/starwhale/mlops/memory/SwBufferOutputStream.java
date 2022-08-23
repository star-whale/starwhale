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

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;

public class SwBufferOutputStream extends OutputStream {
    private final SwBuffer buffer;
    @Getter
    private int offset;

    public SwBufferOutputStream(SwBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(int b) throws IOException {
        if (this.offset >= this.buffer.capacity()) {
            throw new IOException("buffer size limit exceeded");
        }
        this.buffer.setByte(this.offset, (byte) b);
        ++this.offset;
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        if (this.offset + len > this.buffer.capacity()) {
            throw new IOException("buffer size limit exceeded");
        }
        this.buffer.setBytes(this.offset, b, off, len);
        this.offset += len;
    }
}
