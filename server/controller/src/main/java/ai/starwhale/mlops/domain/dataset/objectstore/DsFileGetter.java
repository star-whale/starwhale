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

package ai.starwhale.mlops.domain.dataset.objectstore;

import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
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
public class DsFileGetter {

    static final Set<String> SCHEME_HTTP = Set.of("http", "https");
    final StorageAccessParser storageAccessParser;
    final DatasetVersionMapper datasetVersionMapper;

    final StoragePathCoordinator storagePathCoordinator;

    public DsFileGetter(StorageAccessParser storageAccessParser,
            DatasetVersionMapper datasetVersionMapper, StoragePathCoordinator storagePathCoordinator) {
        this.storageAccessParser = storageAccessParser;
        this.datasetVersionMapper = datasetVersionMapper;
        this.storagePathCoordinator = storagePathCoordinator;
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

    public byte[] dataOf(Long projectId, String datasetName, String uri, Long offset,
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
        String path = findPath(projectId, datasetName, storageUri);
        try (InputStream inputStream = validParam(size, offset) ? storageAccessService.get(path,
                offset, size) : storageAccessService.get(path)) {
            return inputStream.readAllBytes();
        } catch (IOException ioException) {
            log.error("error while accessing storage ", ioException);
            throw new SwProcessException(ErrorType.STORAGE,
                    String.format("error while accessing storage : %s", ioException.getMessage()));
        }
    }

    public String linkOf(Long projectId, String datasetName, String uri, Long expTimeMillis) {
        StorageUri storageUri = getStorageUri(uri);
        if (null != storageUri.getSchema() && SCHEME_HTTP.contains(storageUri.getSchema())) {
            return uri;
        }
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromUri(storageUri);
        String path = findPath(projectId, datasetName, storageUri);
        try {
            return storageAccessService.signedUrl(path, expTimeMillis);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "error while accessing storage", e);
        }
    }

    private String findPath(Long projectId, String datasetName, StorageUri uri) {
        String path = uri.getPathAfterBucket();
        if (StringUtils.hasText(uri.getSchema())) {
            return path;
        }
        String datasetPath = storagePathCoordinator.allocateDatasetPath(projectId, datasetName);
        return StringUtils.trimTrailingCharacter(datasetPath, '/') + "/"
                + StringUtils.trimLeadingCharacter(path, '/');
    }
}
