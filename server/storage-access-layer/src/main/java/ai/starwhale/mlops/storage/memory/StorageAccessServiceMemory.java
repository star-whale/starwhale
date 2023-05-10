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

package ai.starwhale.mlops.storage.memory;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

/**
 * For unit test only.
 */
public class StorageAccessServiceMemory implements StorageAccessService {

    private final Map<String, byte[]> store = new ConcurrentSkipListMap<>();

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        var data = this.store.get(path);
        if (data == null) {
            return new StorageObjectInfo(false, 0L, null);
        }
        return new StorageObjectInfo(true, (long) data.length, null);
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        this.store.put(path, inputStream.readNBytes((int) size));
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        this.store.put(path, body);
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        this.store.put(path, inputStream.readAllBytes());
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        var data = this.store.get(path);
        if (data == null) {
            throw new FileNotFoundException(path);
        }
        return new LengthAbleInputStream(new ByteArrayInputStream(data), data.length);
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        var data = this.store.get(path);
        if (data == null) {
            throw new FileNotFoundException(path);
        }
        return new LengthAbleInputStream(new ByteArrayInputStream(data, offset.intValue(), size.intValue()), size);
    }

    @Override
    public Stream<String> list(String path) throws IOException {
        return this.store.keySet().stream().filter(key -> key.startsWith(path));
    }

    @Override
    public void delete(String path) throws IOException {
        this.store.remove(path);
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) {
        return path;
    }

    @Override
    public String signedPutUrl(String path, Long expTimeMillis) {
        return path;
    }
}
