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

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SystemSettingService implements CommandLineRunner {

    private final YAMLMapper yamlMapper;

    protected final String path;

    @Getter
    protected SystemSetting systemSetting;

    private final StorageAccessService storageAccessService;

    static final String PATH_SETTING = "controller.yaml";

    public SystemSettingService(YAMLMapper yamlMapper,
            StoragePathCoordinator storagePathCoordinator,
            StorageAccessService storageAccessService) {
        this.yamlMapper = yamlMapper;
        this.storageAccessService = storageAccessService;
        this.path = storagePathCoordinator.allocateSystemSettingPath(PATH_SETTING);
    }

    public String querySetting() {
        if (null == systemSetting) {
            return "";
        }
        try {
            return yamlMapper.writeValueAsString(systemSetting);
        } catch (JsonProcessingException e) {
            log.error("write systemSetting setting to yaml failed", e);
            throw new SwProcessException(ErrorType.SYSTEM);
        }
    }

    public String updateSetting(String setting) {
        try {
            systemSetting = yamlMapper.readValue(setting, SystemSetting.class);
        } catch (JsonProcessingException e) {
            log.error("invalid setting yaml {}", setting);
            throw new SwValidationException(ValidSubject.SETTING);
        }
        try {
            storageAccessService.put(path, setting.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("write systemSetting setting to storage failed", e);
            throw new SwProcessException(ErrorType.STORAGE);
        }
        return querySetting();
    }


    @Override
    public void run(String... args) throws Exception {
        StorageObjectInfo head = storageAccessService.head(path);
        if (head.isExists()) {
            try (LengthAbleInputStream lengthAbleInputStream = storageAccessService.get(path)) {
                systemSetting = yamlMapper.readValue(lengthAbleInputStream, SystemSetting.class);
            } catch (JsonProcessingException e) {
                log.error("corrupted system setting yaml");
                throw new SwValidationException(ValidSubject.SETTING);
            }
        }
    }
}
