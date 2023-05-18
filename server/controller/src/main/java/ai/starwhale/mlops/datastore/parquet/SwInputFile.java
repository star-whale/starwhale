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

package ai.starwhale.mlops.datastore.parquet;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

class SwInputFile implements InputFile {

    private final long fileLength;
    private final StorageAccessService storageAccessService;
    private final String path;

    public SwInputFile(StorageAccessService storageAccessService, String path) throws IOException {
        this.storageAccessService = storageAccessService;
        this.path = path;
        this.fileLength = storageAccessService.head(path).getContentLength();
    }

    @Override
    public long getLength() {
        return this.fileLength;
    }

    @Override
    public SeekableInputStream newStream() {
        return new SeekableInputStream() {
            private long pos;
            private LengthAbleInputStream input;
            private final byte[] copyBuf = new byte[4096];

            @Override
            public long getPos() {
                return this.pos;
            }

            @Override
            public void seek(long newPos) throws IOException {
                if (newPos > fileLength) {
                    throw new IndexOutOfBoundsException(
                            MessageFormat.format("length={0}, pos={1}", fileLength, newPos));
                }
                if (newPos != this.pos) {
                    this.pos = newPos;
                    this.closeInput();
                }
            }

            @Override
            public int read() throws IOException {
                if (this.pos == fileLength) {
                    return -1;
                }
                if (this.input == null) {
                    this.initInput(this.pos, 4096);
                }
                int ret = this.input.read();
                if (ret < 0) {
                    this.initInput(this.pos, 4096);
                    ret = this.input.read();
                }
                ++this.pos;
                return ret;
            }

            @Override
            public int read(ByteBuffer byteBuffer) throws IOException {
                var toRead = (int) Math.min(fileLength - this.pos, byteBuffer.remaining());
                if (toRead < byteBuffer.remaining()) {
                    var buf = byteBuffer.slice();
                    buf.limit(byteBuffer.position() + toRead);
                    this.readFully(buf);
                    byteBuffer.position(buf.position());
                } else {
                    this.readFully(byteBuffer);
                }
                return toRead;
            }

            @Override
            public void readFully(byte[] bytes) throws IOException {
                this.readFully(bytes, 0, bytes.length);
            }

            @Override
            public void readFully(byte[] bytes, int offset, int len) throws IOException {
                if (this.pos + len > fileLength) {
                    throw new EOFException(
                            MessageFormat.format("try to read {0} bytes, but only {1} bytes remains",
                                    len, fileLength - this.pos));
                }
                if (this.input == null) {
                    this.initInput(this.pos, len);
                }
                var actual = this.input.readNBytes(bytes, offset, len);
                if (actual < len) {
                    this.initInput(this.pos + actual, len - actual);
                    this.input.readNBytes(bytes, offset + actual, len - actual);
                }
                this.pos += len;
            }

            @Override
            public void readFully(ByteBuffer byteBuffer) throws IOException {
                if (byteBuffer.hasArray()) {
                    this.readFully(byteBuffer.array(),
                            byteBuffer.arrayOffset() + byteBuffer.position(),
                            byteBuffer.remaining());
                    byteBuffer.position(byteBuffer.limit());
                    return;
                }
                var toRead = byteBuffer.remaining();
                if (this.pos + toRead > fileLength) {
                    throw new EOFException(
                            MessageFormat.format("try to read {0} bytes, but only {1} bytes remains",
                                    toRead, fileLength - this.pos));
                }
                if (this.input == null) {
                    this.initInput(this.pos, toRead);
                }
                var actual = this.copy(this.input, byteBuffer);
                if (actual < toRead) {
                    this.initInput(this.pos + actual, toRead - actual);
                    this.copy(this.input, byteBuffer);
                }
                this.pos += toRead;
            }

            @Override
            public void close() throws IOException {
                this.closeInput();
            }

            private int copy(InputStream in, ByteBuffer buffer) throws IOException {
                int length = buffer.remaining();
                while (buffer.remaining() > 0) {
                    var toRead = Math.min(this.copyBuf.length, buffer.remaining());
                    int count = in.read(this.copyBuf, 0, toRead);
                    if (count == -1) { // EOF
                        break;
                    }
                    buffer.put(this.copyBuf, 0, toRead);
                }
                return length - buffer.remaining();
            }

            private void initInput(long pos, long size) throws IOException {
                this.closeInput();
                // https://help.aliyun.com/document_detail/31980.html
                // size can not be larger than the lasts content length
                size = Math.min(Math.max(size, 4096), fileLength - pos);
                this.input = storageAccessService.get(path, pos, size);
            }

            private void closeInput() throws IOException {
                if (this.input != null) {
                    this.input.close();
                }
                this.input = null;
            }
        };
    }
}
