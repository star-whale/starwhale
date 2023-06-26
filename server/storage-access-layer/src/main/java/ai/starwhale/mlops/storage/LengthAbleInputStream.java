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

package ai.starwhale.mlops.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class LengthAbleInputStream extends InputStream {

    private final InputStream inputStream;

    private final long size;

    private long remaining;

    public LengthAbleInputStream(InputStream inputStream, long size) {
        this.inputStream = inputStream;
        this.size = size;
        this.remaining = size;
    }

    @Override
    public int read() throws IOException {
        if (this.remaining == 0) {
            return -1;
        }
        var ret = this.inputStream.read();
        if (ret < 0) {
            this.remaining = 0;
        } else {
            --this.remaining;
        }
        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.remaining == 0) {
            return -1;
        }
        len = (int) Math.min(this.remaining, len);
        var ret = this.inputStream.read(b, off, len);
        if (ret < 0) {
            this.remaining = 0;
        } else {
            this.remaining -= ret;
        }
        return ret;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return this.readNBytes(Integer.MAX_VALUE);
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        if (this.remaining == 0 || len == 0) {
            return new byte[0];
        }
        len = (int) Math.min(this.remaining, len);
        var ret = this.inputStream.readNBytes(len);
        this.remaining -= ret.length;
        return ret;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (this.remaining == 0 || len == 0) {
            return 0;
        }
        len = (int) Math.min(this.remaining, len);
        var ret = this.inputStream.readNBytes(b, off, len);
        this.remaining -= ret;
        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        n = Math.min(this.remaining, n);
        var ret = this.inputStream.skip(n);
        this.remaining -= ret;
        return ret;
    }

    @Override
    public int available() throws IOException {
        return this.inputStream.available();
    }

    @Override
    public void close() throws IOException {
        this.inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        this.inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        this.inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return this.inputStream.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return this.inputStream.transferTo(out);
    }

    public long getSize() {
        return this.size;
    }
}
