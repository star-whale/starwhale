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
import ai.starwhale.mlops.domain.dataset.build.po.BuildRecordEntity;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class BuildRecordGarbageCollector {

    private final BuildRecordMapper mapper;

    private final StorageAccessService storageAccessService;

    public BuildRecordGarbageCollector(BuildRecordMapper mapper, StorageAccessService storageAccessService) {
        this.mapper = mapper;
        this.storageAccessService = storageAccessService;
    }

    @Scheduled(cron = "${sw.dataset.build.gc-rate:0 0 0 * * ?}")
    public void gc() {
        var finished = mapper.selectFinishedAndUncleaned();
        for (BuildRecordEntity finishedRecord : finished) {
            try {
                var files = storageAccessService.list(finishedRecord.getStoragePath())
                        .collect(Collectors.toList());
                for (String file : files) {
                    storageAccessService.delete(file);
                }
                mapper.updateCleaned(finishedRecord.getId());
            } catch (IOException e) {
                log.warn("delete storage file path:{} failed", finishedRecord.getStoragePath(), e);
            }
        }
    }
}
