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

package ai.starwhale.mlops.domain.swds.objectstore;

import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
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

    final SwDatasetVersionMapper swDatasetVersionMapper;

    public DsFileGetter(StorageAccessParser storageAccessParser,
            SwDatasetVersionMapper swDatasetVersionMapper) {
        this.storageAccessParser = storageAccessParser;
        this.swDatasetVersionMapper = swDatasetVersionMapper;
    }

    public byte[] dataOf(Long datasetId, String uri, String authName, String offset,
            String size) {
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromAuth(
                datasetId, uri, authName);
        String path = new StorageUri(uri).getPath();
        StorageObjectInfo objectInfo;
        try {
            objectInfo = storageAccessService.head(path);
        } catch (IOException e) {
            log.error("error while accessing storage ", e);
            throw new SwProcessException(ErrorType.STORAGE).tip(
                    String.format("error while accessing storage : %s", e.getMessage()));
        }
        if (!objectInfo.isExists()) {
            SwDatasetVersionEntity versionById = swDatasetVersionMapper.getVersionById(datasetId);
            path = versionById.getStoragePath() + "/" + path;
        }
        try (InputStream inputStream = storageAccessService.get(path,
                (long) ColumnType.INT64.decode(offset), (long) ColumnType.INT64.decode(size))) {
            return inputStream.readAllBytes();
        } catch (IOException ioException) {
            log.error("error while accessing storage ", ioException);
            throw new SwProcessException(ErrorType.STORAGE).tip(
                    String.format("error while accessing storage : %s", ioException.getMessage()));
        }
    }
}
