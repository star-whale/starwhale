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

package ai.starwhale.mlops.datastore;

import ai.starwhale.mlops.memory.SwBuffer;
import ai.starwhale.mlops.memory.SwBufferInputStream;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.springframework.stereotype.Component;

public class ObjectStore {

    private final SwBufferManager bufferManager;

    private final StorageAccessService storageAccessService;

    public ObjectStore(SwBufferManager bufferManager, StorageAccessService storageAccessService) {
        this.bufferManager = bufferManager;
        this.storageAccessService = storageAccessService;
    }

    public Iterator<String> list(String prefix) throws IOException {
        return this.storageAccessService.list(prefix).iterator();
    }

    public void put(String name, SwBuffer buf) throws IOException {
        this.storageAccessService.put(name, new SwBufferInputStream(buf), buf.capacity());
    }

    public SwBuffer get(String name) throws IOException {
        try (var is = this.storageAccessService.get(name)) {
            var ret = this.bufferManager.allocate(Math.toIntExact(is.getSize()));
            int read = is.readNBytes(ret.asByteBuffer().array(), 0, ret.capacity());
            assert read == ret.capacity();
            return ret;
        }
    }

    public void delete(String name) throws IOException {
        this.storageAccessService.delete(name);
    }
}
