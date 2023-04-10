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

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SwOutputFile implements OutputFile {

    private final StorageAccessService storageAccessService;
    private final String path;

    public SwOutputFile(StorageAccessService storageAccessService, String path) {
        this.storageAccessService = storageAccessService;
        this.path = path;
    }

    @Override
    public PositionOutputStream create(long blockSizeHint) throws IOException {
        return this.createOrOverwrite(blockSizeHint);
    }

    @Override
    public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
        var out = new PipedOutputStream();
        var in = new PipedInputStream(out);
        var exception = new AtomicReference<IOException>();
        var t = new Thread(() -> {
            try {
                this.storageAccessService.put(this.path, in);
            } catch (IOException e) {
                log.error("fail to write the storage", e);
                try {
                    out.close();
                    in.close();
                } catch (IOException ex) {
                    // ignore this
                }
                exception.set(e);
            }
        });
        t.start();
        return new PositionOutputStream() {
            private long pos;

            @Override
            public long getPos() {
                return this.pos;
            }

            @Override
            public void write(int b) throws IOException {
                out.write(b);
                ++this.pos;
            }

            @Override
            public void write(@NotNull byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
                this.pos += len;
            }

            @Override
            public void close() throws IOException {
                out.close();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
                if (exception.get() != null) {
                    throw exception.get();
                }
            }
        };
    }

    @Override
    public boolean supportsBlockSize() {
        return false;
    }

    @Override
    public long defaultBlockSize() {
        return 0;
    }
}
