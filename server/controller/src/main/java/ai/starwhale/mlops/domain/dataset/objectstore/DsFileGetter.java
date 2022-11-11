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

import ai.starwhale.mlops.datastore.ColumnTypeScalar;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.StorageUri;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DsFileGetter {

    final StorageAccessParser storageAccessParser;

    final DatasetVersionMapper datasetVersionMapper;

    public DsFileGetter(StorageAccessParser storageAccessParser,
            DatasetVersionMapper datasetVersionMapper) {
        this.storageAccessParser = storageAccessParser;
        this.datasetVersionMapper = datasetVersionMapper;
    }

    public byte[] dataOf(Long datasetId, String uri, String authName, Long offset,
            Long size) {
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromAuth(
                datasetId, uri, authName);
        String path = checkPath(datasetId, uri, storageAccessService);
        try (InputStream inputStream = validParam(size, offset) ? storageAccessService.get(path,
                offset, size) : storageAccessService.get(path)) {
            return inputStream.readAllBytes();
        } catch (IOException ioException) {
            log.error("error while accessing storage ", ioException);
            throw new SwProcessException(ErrorType.STORAGE,
                    String.format("error while accessing storage : %s", ioException.getMessage()));
        }
    }

    public String linkOf(Long datasetId, String uri, String authName, Long expTimeMillis) {
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromAuth(
                datasetId, uri, authName);
        String path = checkPath(datasetId, uri, storageAccessService);
        try {
            return storageAccessService.signedUrl(path, expTimeMillis);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "error while accessing storage", e);
        }
    }

    private String checkPath(Long datasetId, String uri, StorageAccessService storageAccessService) {
        String path = new StorageUri(uri).getPath();
        StorageObjectInfo objectInfo;
        try {
            objectInfo = storageAccessService.head(path);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "error while accessing storage", e);
        }
        if (!objectInfo.isExists()) {
            DatasetVersionEntity versionById = datasetVersionMapper.getVersionById(datasetId);
            path = versionById.getStoragePath() + "/" + path;
        }
        return path;
    }

    private static boolean validParam(long sizeLong, long offsetLong) {
        return sizeLong > 0 && offsetLong >= 0;
    }
}
