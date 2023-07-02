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

package ai.starwhale.mlops.domain.blob;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CachedBlobService implements BlobService {

    private final Map<String, BlobService> caches = new HashMap<>();
    private final BlobService defaultBlobService;

    public CachedBlobService(StorageAccessService defaultStorageAccessService,
            BlobServiceConfig config) {
        this.defaultBlobService = new BlobServiceImpl(defaultStorageAccessService, config.getDataRootPath(),
                config.getUrlExpirationTime());
        for (var cacheConfig : config.getCaches()) {
            StorageAccessService storageAccessService;
            switch (cacheConfig.getStorageType()) {
                case "s3":
                    storageAccessService = new StorageAccessServiceS3(cacheConfig);
                    break;
                case "aliyun":
                    storageAccessService = new StorageAccessServiceAliyun(cacheConfig);
                    break;
                case "memory":
                    // for test only
                    storageAccessService = new StorageAccessServiceMemory();
                    break;
                default:
                    throw new RuntimeException("invalid cache storage type: " + cacheConfig.getStorageType());
            }
            this.caches.put(cacheConfig.getBlobIdPrefix(),
                    new BlobServiceImpl(storageAccessService,
                            cacheConfig.getBlobRoot(),
                            config.getUrlExpirationTime()));
        }
    }

    @Override
    public String generateBlobId() throws IOException {
        return this.defaultBlobService.generateBlobId();
    }

    @Override
    public String readBlobRef(String contentMd5, long contentLength) throws IOException {
        return this.defaultBlobService.readBlobRef(contentMd5, contentLength);
    }

    @Override
    public String generateBlobRef(String blobId) {
        return this.defaultBlobService.generateBlobRef(blobId);
    }

    public LengthAbleInputStream readBlob(String blobId) throws IOException {
        return this.getBlobServiceByBlobId(blobId).readBlob(blobId);
    }

    public LengthAbleInputStream readBlob(String blobId, long offset, long length) throws IOException {
        return this.getBlobServiceByBlobId(blobId).readBlob(blobId, offset, length);
    }

    public byte[] readBlobAsByteArray(String blobId) throws IOException {
        return this.getBlobServiceByBlobId(blobId).readBlobAsByteArray(blobId);
    }

    public String getSignedUrl(String blobId) throws IOException {
        return this.getBlobServiceByBlobId(blobId).getSignedUrl(blobId);
    }


    public String getSignedPutUrl(String blobId) throws IOException {
        return this.getBlobServiceByBlobId(blobId).getSignedPutUrl(blobId);
    }

    public BlobService getBlobServiceByBlobId(String blobId) {
        int index = blobId.indexOf('-');
        if (index < 0) {
            return this.defaultBlobService;
        }
        var ret = this.caches.get(blobId.substring(0, index));
        if (ret == null) {
            throw new IllegalArgumentException("invalid blob id: " + blobId);
        }
        return ret;
    }
}
