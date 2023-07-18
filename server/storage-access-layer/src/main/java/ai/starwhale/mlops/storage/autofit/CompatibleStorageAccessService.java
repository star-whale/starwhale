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

package ai.starwhale.mlops.storage.autofit;

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import lombok.experimental.Delegate;

/**
 * if it is compatible with an uri
 */
public abstract class CompatibleStorageAccessService implements StorageAccessService {

    @Delegate
    protected final StorageAccessService storageAccessService;

    public CompatibleStorageAccessService(StorageAccessService storageAccessService) {
        this.storageAccessService = storageAccessService;
    }

    /**
     * if it is compatible with an uri
     *
     * @param uri the uri to be tested
     * @return if it is compatible with an uri
     */
    public abstract boolean compatibleWith(StorageUri uri);
}
