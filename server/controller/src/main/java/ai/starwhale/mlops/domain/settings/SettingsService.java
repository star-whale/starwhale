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

package ai.starwhale.mlops.domain.settings;

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.domain.settings.mapper.SettingsMapper;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SettingsService {

    private final UserService userService;
    private final SettingsMapper settingsMapper;

    public SettingsService(UserService userService, SettingsMapper settingsMapper) {
        this.userService = userService;
        this.settingsMapper = settingsMapper;
    }

    public String queryUserSettingsString() {
        User user = userService.currentUserDetail();
        var settingsEntity = settingsMapper.get(Scope.USER, user.getId());
        return settingsEntity == null ? null : settingsEntity.getContent();
    }

    public Settings queryUserSettings() {
        try {
            User user = userService.currentUserDetail();
            var settingsEntity = settingsMapper.get(Scope.USER, user.getId());
            return settingsEntity == null ? Settings.builder().build() :
                    Constants.yamlMapper.readValue(settingsEntity.getContent(), Settings.class);
        } catch (JsonProcessingException e) {
            log.error("parse settings to yaml failed", e);
            throw new SwProcessException(ErrorType.INFRA);
        }
    }

    @Transactional
    public boolean updateUserSettings(String settings) {
        try {
            // for valid
            Constants.yamlMapper.readValue(settings, Settings.class);
            User user = userService.currentUserDetail();
            var entity = settingsMapper.get(Scope.USER, user.getId());
            if (entity != null) {
                return settingsMapper.update(entity.getId(), settings) > 0;
            } else {
                return settingsMapper.insert(user.getId(), settings, Scope.USER) > 0;
            }
        } catch (JsonProcessingException e) {
            log.error("invalid setting yaml {}", settings, e);
            throw new SwValidationException(ValidSubject.SETTING);
        }
    }
}
