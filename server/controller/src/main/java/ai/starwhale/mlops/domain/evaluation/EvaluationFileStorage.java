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

package ai.starwhale.mlops.domain.evaluation;

import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.storage.HashNamedObjectStore;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.UriAccessor;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Component
public class EvaluationFileStorage {
    private final StoragePathCoordinator storagePathCoordinator;
    private final StorageAccessService storageAccessService;
    private final ProjectService projectService;
    private final UriAccessor uriAccessor;

    public EvaluationFileStorage(
            StoragePathCoordinator storagePathCoordinator,
            StorageAccessService storageAccessService,
            ProjectService projectService, UriAccessor uriAccessor) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.projectService = projectService;
        this.uriAccessor = uriAccessor;
    }

    public HashNamedObjectStore hashObjectStore(String project, String version) {
        return new HashNamedObjectStore(storageAccessService,
                storagePathCoordinator.allocateEvalStoragePath(projectService.getProjectId(project), version));
    }

    public String uploadHashedBlob(String projectUrl, String version, MultipartFile file, String blobHash) {
        Long projectId = projectService.findProject(projectUrl).getId();
        String storagePath = storagePathCoordinator.allocateEvalStoragePath(projectId, version);
        HashNamedObjectStore hashNamedObjectStore = new HashNamedObjectStore(storageAccessService, storagePath);
        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            return hashNamedObjectStore.put(StringUtils.hasText(blobHash) ? blobHash : filename, inputStream);
        } catch (IOException e) {
            log.error("write eval blob file failed {}", filename, e);
            throw new SwProcessException(SwProcessException.ErrorType.NETWORK, "write eval blob file failed", e);
        }
    }

    public byte[] dataOf(String uri, Long offset, Long size) {
        return uriAccessor.dataOf(uri, offset, size);
    }

    public String signLink(String uri, Long expTimeMillis) {
        return uriAccessor.linkOf(uri, expTimeMillis);
    }

    public Map<String, String> signLinks(Set<String> uris, Long expTimeMillis) {
        return uris.stream().collect(Collectors.toMap(u -> u, u -> {
            try {
                return signLink(u, expTimeMillis);
            } catch (Exception e) {
                return "";
            }
        }));
    }
}
