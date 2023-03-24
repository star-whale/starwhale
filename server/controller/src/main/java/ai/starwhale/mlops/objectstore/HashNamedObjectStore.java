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

package ai.starwhale.mlops.objectstore;

import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.StringUtils;

public class HashNamedObjectStore {

    private final StorageAccessService storageAccessService;

    private final String rootPath;

    public HashNamedObjectStore(StorageAccessService storageAccessService, String rootPath) {
        this.storageAccessService = storageAccessService;
        this.rootPath = rootPath;
    }

    public String put(String blobHash, InputStream file) throws IOException {
        String absolutePath = absolutePath(blobHash);
        StorageObjectInfo storageObjectInfo = storageAccessService.head(absolutePath);
        if (!storageObjectInfo.isExists()) {
            storageAccessService.put(absolutePath, file);
        }
        return relativePath(blobHash);
    }

    public InputStream get(String blobHash) throws IOException {
        return storageAccessService.get(absolutePath(blobHash));
    }

    public StorageObjectInfo head(String blobHash) throws IOException {
        return storageAccessService.head(absolutePath(blobHash));
    }

    public String relativePath(String blobHash) {
        if (null == blobHash || blobHash.length() < 3) {
            throw new SwValidationException(ValidSubject.OBJECT_STORE, "file blobHash should have at least 3 chars");
        }
        return blobHash.substring(0, 2) + SLASH + blobHash;
    }

    private static final String SLASH = "/";

    public String absolutePath(String blobHash) {
        return StringUtils.trimTrailingCharacter(rootPath, '/') + SLASH + relativePath(blobHash);
    }

}
