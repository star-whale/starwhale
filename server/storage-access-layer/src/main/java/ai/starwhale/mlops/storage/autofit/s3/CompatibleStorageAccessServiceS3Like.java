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

package ai.starwhale.mlops.storage.autofit.s3;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessService;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Stream;

/**
 * provides file upload/ download /list services Compilable
 */
public class CompatibleStorageAccessServiceS3Like extends CompatibleStorageAccessService {

    final S3Config s3Config;

    final Set<String> schemas;

    public boolean compatibleWith(StorageUri uri) {
        uri.getSchema();
        uri.getBucket();
        uri.getHost();
        uri.getPort();
        return false;
    }

    public CompatibleStorageAccessServiceS3Like(StorageAccessService storageAccessService, S3Config s3Config,
            Set<String> schemas) {
        super(storageAccessService);
        this.s3Config = s3Config;
        this.schemas = schemas;
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        return storageAccessService.head(path);
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        storageAccessService.put(path, inputStream, size);
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        storageAccessService.put(path, body);
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        storageAccessService.put(path, inputStream);
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        return storageAccessService.get(path);
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        return storageAccessService.get(path, offset, size);
    }

    @Override
    public Stream<String> list(String path) throws IOException {
        return storageAccessService.list(path);
    }

    @Override
    public void delete(String path) throws IOException {
        storageAccessService.delete(path);
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) throws IOException {
        return storageAccessService.signedUrl(path, expTimeMillis);
    }


}
