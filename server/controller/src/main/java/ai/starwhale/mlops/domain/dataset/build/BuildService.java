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

package ai.starwhale.mlops.domain.dataset.build;

import ai.starwhale.mlops.domain.dataset.build.mapper.BuildRecordMapper;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BuildService {
    private static final String PREFIX_FORMAT = "%s/%s/%s/";
    private final StorageAccessService storageAccessService;

    private final BuildRecordMapper mapper;

    public BuildService(StorageAccessService storageAccessService, BuildRecordMapper mapper) {
        this.storageAccessService = storageAccessService;
        this.mapper = mapper;
    }

    /**
     * generate path prefix for build(use current oss service)
     * @return
     */
    public String generatePathPrefix() {
        // TODO use current oss service
        return String.format(PREFIX_FORMAT, "datasets", "build", UUID.randomUUID());
    }

    /**
     * generate signed urls for files(use the special oss which parsed from path prefix)
     * @param pathPrefix path prefix
     * @param files files to be signed.
     * @return signed urls for files, key is file name, value is signed url.
     */
    public Map<String, String> generateSignedUrls(String pathPrefix, Set<String> files) {
        return null;
    }

    public Map<String, String> getSignedUrls(Long buildId) {
        var record = mapper.get(buildId);
        return this.getSignedUrls(record.getStoragePath());
    }

    /**
     * get signed urls for files(use the special oss which parsed from path prefix)
     * @param pathPrefix path prefix
     * @return signed urls for files, key is file name, value is signed url.
     */
    private Map<String, String> getSignedUrls(String pathPrefix) {
        return null;
    }
}
