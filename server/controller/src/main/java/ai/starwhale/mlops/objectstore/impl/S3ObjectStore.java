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
package ai.starwhale.mlops.objectstore.impl;

import ai.starwhale.mlops.memory.SwBuffer;
import ai.starwhale.mlops.memory.SwBufferInputStream;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.objectstore.ObjectStore;
import ai.starwhale.mlops.storage.StorageAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.Iterator;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "s3", matchIfMissing = true)
public class S3ObjectStore implements ObjectStore {
    private final SwBufferManager bufferManager;

    private final StorageAccessService storageAccessService;

    public S3ObjectStore(SwBufferManager bufferManager, StorageAccessService storageAccessService) {
        this.bufferManager = bufferManager;
        this.storageAccessService = storageAccessService;
    }

    @Override
    public Iterator<String> list(String prefix) throws IOException {
        return this.storageAccessService.list(prefix).iterator();
    }

    @Override
    public void put(String name, SwBuffer buf) throws IOException {
        this.storageAccessService.put(name, new SwBufferInputStream(buf), buf.capacity());
    }

    @Override
    public SwBuffer get(String name) throws IOException {
        @SuppressWarnings("unchecked")
        var result = (ResponseInputStream<GetObjectResponse>) this.storageAccessService.get(name);
        var ret = this.bufferManager.allocate(result.response().contentLength().intValue());
        assert result.read(ret.asByteBuffer().array()) == ret.capacity();
        return ret;
    }

    @Override
    public void delete(String name) throws IOException {
        this.storageAccessService.delete(name);
    }
}
