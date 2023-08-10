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


import ai.starwhale.mlops.api.protobuf.Storage.FlattenFileVo;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class StorageService {

    private final StorageAccessService storageAccessService;

    public StorageService(StorageAccessService storageAccessService) {
        this.storageAccessService = storageAccessService;
    }

    public List<FlattenFileVo> listStorageFile(String storagePath) throws IOException {
        if (!StringUtils.hasText(storagePath)) {
            log.error("Cannot list storage files. Storage path is empty");
            return List.of();
        }

        try {
            Stream<String> list = storageAccessService.list(storagePath);
            return list.map(filePath -> {
                long length = 0L;
                try {
                    StorageObjectInfo info = storageAccessService.head(filePath);
                    length = info.getContentLength();
                } catch (IOException e) {
                    log.error("storage head", e);
                }
                if (StrUtil.startWith(filePath, storagePath)) {
                    filePath = filePath.substring(storagePath.length() + 1);
                }
                return FlattenFileVo.newBuilder()
                        .setName(filePath)
                        .setSize(FileUtil.readableFileSize(length))
                        .build();
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE, "list swmp storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public long getStorageSize(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            log.error("Cannot list storage files. Storage path is empty");
            return 0L;
        }

        try {
            Stream<String> list = storageAccessService.list(storagePath);
            final AtomicLong length = new AtomicLong(0);
            list.forEach(filePath -> {
                try {
                    StorageObjectInfo info = storageAccessService.head(filePath);
                    length.addAndGet(info.getContentLength());
                } catch (IOException e) {
                    log.error("storage head", e);
                }
            });
            return length.get();
        } catch (IOException e) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE, "list swmp storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
