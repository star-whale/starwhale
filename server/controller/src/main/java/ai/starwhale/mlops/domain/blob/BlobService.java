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

import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Service;

@Service
public class BlobService {

    private final StorageAccessService storageAccessService;

    private final String dataRootPath;
    private final long urlExpirationTimeMillis;

    private final String refRootPath;

    private final Random random = new Random();

    public BlobService(StorageAccessService storageAccessService,
            @Value("${sw.blob-service.data-root-path:/blob/}") String dataRootPath,
            @Value("${sw.blob-service.url-expiration-time:4h}") String urlExpirationTime) {
        this.storageAccessService = storageAccessService;
        if (dataRootPath.endsWith("/")) {
            this.dataRootPath = dataRootPath;
        } else {
            this.dataRootPath = dataRootPath + "/";
        }
        this.refRootPath = this.dataRootPath + "ref/";
        this.urlExpirationTimeMillis = DurationStyle.detectAndParse(urlExpirationTime).toMillis();
    }

    private String blobIdLongToString(long blobId) {
        return String.format("%016X", blobId);
    }

    public String generateBlobId() throws IOException {
        for (; ; ) {
            var blobId = this.random.nextLong();
            var ret = this.blobIdLongToString(blobId);
            if (blobId != 0 && !this.storageAccessService.head(this.getObjectPath(ret)).isExists()) {
                return ret;
            }
        }
    }

    public String readBlobRef(String contentMd5, long contentLength) throws IOException {
        try (var data = this.storageAccessService.get(this.getBlobRefPath(contentMd5, contentLength))) {
            return new String(data.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String generateBlobRef(String blobId) {
        try {
            var info = this.storageAccessService.head(this.getObjectPath(blobId), true);
            if (!info.isExists()) {
                throw new SwValidationException(ValidSubject.MODEL, "blob not found: " + blobId);
            }
            var refPath = this.getBlobRefPath(info.getMd5sum(), info.getContentLength());
            try (var data = this.storageAccessService.get(refPath)) {
                return new String(data.readAllBytes(), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                this.storageAccessService.put(refPath, blobId.getBytes(StandardCharsets.UTF_8));
                return blobId;
            }
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "", e);
        }
    }

    public LengthAbleInputStream readBlob(String blobId) throws IOException {
        return this.storageAccessService.get(this.getObjectPath(blobId));
    }

    public LengthAbleInputStream readBlob(String blobId, long offset, long length) throws IOException {
        if (offset == 0 && length == 0) {
            return this.storageAccessService.get(this.getObjectPath(blobId));
        }
        return this.storageAccessService.get(this.getObjectPath(blobId), offset, length);
    }

    public byte[] readBlobAsByteArray(String blobId) throws IOException {
        try (var in = this.readBlob(blobId)) {
            return in.readAllBytes();
        }
    }

    public String getSignedUrl(String blobId) throws IOException {
        return this.storageAccessService.signedUrl(this.getObjectPath(blobId), this.urlExpirationTimeMillis);
    }


    public String getSignedPutUrl(String blobId) throws IOException {
        return this.storageAccessService.signedPutUrl(this.getObjectPath(blobId), this.urlExpirationTimeMillis);
    }

    private String getObjectPath(String blobId) {
        return this.dataRootPath + blobId;
    }

    private String getBlobRefPath(String contentMd5, long contentLength) {
        return this.refRootPath + contentMd5.toLowerCase() + "-" + Long.toHexString(contentLength);
    }
}
