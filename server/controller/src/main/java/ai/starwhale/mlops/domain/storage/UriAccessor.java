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

package ai.starwhale.mlops.domain.storage;

import ai.starwhale.mlops.domain.dataset.objectstore.StorageAccessParser;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class UriAccessor {

    static final Set<String> SCHEME_HTTP = Set.of("http", "https");
    final StorageAccessParser storageAccessParser;

    public UriAccessor(StorageAccessParser storageAccessParser) {
        this.storageAccessParser = storageAccessParser;
    }

    @NotNull
    private static StorageUri getStorageUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            throw new SwValidationException(ValidSubject.DATASET, "uri is empty");
        }
        StorageUri storageUri;
        try {
            storageUri = new StorageUri(uri);
        } catch (URISyntaxException e) {
            log.error("malformed uri", e);
            throw new SwValidationException(ValidSubject.DATASET, "malformed uri");
        }
        return storageUri;
    }

    private static boolean validParam(long sizeLong, long offsetLong) {
        return sizeLong > 0 && offsetLong >= 0;
    }

    public byte[] dataOf(String uri, Long offset,
            Long size) {
        StorageUri storageUri = getStorageUri(uri);
        if (null != storageUri.getSchema() && SCHEME_HTTP.contains(storageUri.getSchema())) {
            try (InputStream is = new URL(uri).openStream()) {
                return is.readAllBytes();
            } catch (MalformedURLException e) {
                log.error("malformated url {}", uri, e);
                throw new SwValidationException(ValidSubject.DATASET, "malformated url", e);
            } catch (IOException e) {
                log.error("connection to uri failed {}", uri, e);
                throw new SwProcessException(ErrorType.NETWORK, "connection to uri failed", e);
            }
        }
        StorageAccessService storageAccessService =
                storageAccessParser.getStorageAccessServiceFromUri(getStorageUri(uri));
        try (InputStream inputStream = validParam(size, offset) ? storageAccessService.get(
                storageUri.getPathAfterBucket(),
                offset, size) : storageAccessService.get(storageUri.getPathAfterBucket())) {
            return inputStream.readAllBytes();
        } catch (IOException ioException) {
            log.error("error while accessing storage ", ioException);
            throw new SwProcessException(ErrorType.STORAGE,
                    String.format("error while accessing storage : %s", ioException.getMessage()));
        }
    }

    public String linkOf(String uri, Long expTimeMillis) {
        StorageUri storageUri = getStorageUri(uri);
        if (null != storageUri.getSchema() && SCHEME_HTTP.contains(storageUri.getSchema())) {
            return uri;
        }
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromUri(storageUri);
        try {
            return storageAccessService.signedUrl(storageUri.getPathAfterBucket(), expTimeMillis);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "error while accessing storage", e);
        }
    }

}
