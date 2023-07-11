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

package ai.starwhale.mlops.domain.filestorage;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class FileStorageService {
    private final StorageAccessService storageAccessService;
    private final String dataRootPath;
    private final Long urlExpirationTimeMillis;
    private final Integer maxFileNum;

    public FileStorageService(StorageAccessService storageAccessService,
                              @Value("${sw.filestorage.data-root-path:dataset-build/}") String dataRootPath,
                              @Value("${sw.filestorage.url-expiration-time:24h}") String urlExpirationTime,
                              @Value("${sw.filestorage.max-signed-files:1000}") Integer maxFileNum) {
        this.storageAccessService = storageAccessService;
        if (dataRootPath.endsWith("/")) {
            this.dataRootPath = dataRootPath;
        } else {
            this.dataRootPath = dataRootPath + "/";
        }
        this.urlExpirationTimeMillis = DurationStyle.detectAndParse(urlExpirationTime).toMillis();
        this.maxFileNum = maxFileNum;
    }

    /**
     * generate path prefix for build(use current oss service)
     *
     * @return path
     */
    public String generatePathPrefix() {
        return String.format("%s%s/", dataRootPath, UUID.randomUUID());
    }

    /**
     * validate path prefix
     *
     * @param pathPrefix path prefix
     * @return true if valid, false if not
     */
    public boolean validatePathPrefix(String pathPrefix) {
        return pathPrefix.startsWith(dataRootPath);
    }

    /**
     * generate signed urls for files(use the special oss which parsed from path prefix)
     *
     * @param pathPrefix path prefix
     * @param files      files to be signed.
     * @return signed urls for files, key is file name, value is signed url.
     */
    public Map<String, String> generateSignedPutUrls(String pathPrefix, Set<String> files) {
        if (files.size() > maxFileNum) {
            throw new SwValidationException(SwValidationException.ValidSubject.OBJECT_STORE, "file count is too large");
        }
        Map<String, String> signedUrls = new HashMap<>();
        for (String file : files) {
            try {
                var url = storageAccessService.signedPutUrl(
                        pathPrefix + file, APPLICATION_OCTET_STREAM_VALUE, urlExpirationTimeMillis);
                signedUrls.put(file, url);
            } catch (IOException e) {
                log.error("generate signed put url error", e);
                throw new SwProcessException(SwProcessException.ErrorType.STORAGE, e.getMessage());
            }
        }
        return signedUrls;
    }


    /**
     * get signed urls for files(use the special oss which parsed from path prefix)
     *
     * @param pathPrefix path prefix
     * @return signed urls for files, key is file name, value is signed url.
     */
    public Map<String, String> generateSignedGetUrls(String pathPrefix) {
        if (!validatePathPrefix(pathPrefix)) {
            throw new SwValidationException(SwValidationException.ValidSubject.OBJECT_STORE, "pathPrefix is invalid");
        }
        try {
            return storageAccessService.list(pathPrefix)
                    .collect(Collectors.toMap(
                        filePath -> filePath.substring(pathPrefix.length() + 1),
                        filePath -> {
                            try {
                                return storageAccessService.signedUrl(filePath, urlExpirationTimeMillis);
                            } catch (IOException e) {
                                log.error("file:{} generate signed get url error", filePath, e);
                                throw new SwProcessException(SwProcessException.ErrorType.STORAGE, e.getMessage());
                            }
                        }));
        } catch (IOException e) {
            log.error("path:{} list files error", pathPrefix, e);
            throw new SwProcessException(SwProcessException.ErrorType.STORAGE, e.getMessage());
        }
    }
}
