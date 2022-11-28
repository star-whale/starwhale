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

package ai.starwhale.mlops.domain.panel;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.panel.mapper.PanelSettingMapper;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class PanelSettingService {

    private final UserService userService;
    private final PanelSettingMapper panelSettingMapper;
    private final IdConverter idConvertor;

    PanelSettingService(
            UserService userService,
            PanelSettingMapper panelSettingMapper,
            IdConverter idConvertor
    ) {
        this.userService = userService;
        this.panelSettingMapper = panelSettingMapper;
        this.idConvertor = idConvertor;
    }

    public void saveSetting(String projectIdStr, String name, String content) {
        User user = userService.currentUserDetail();
        var projectId = idConvertor.revert(projectIdStr);
        panelSettingMapper.set(user.getId(), projectId, name, content);
    }

    public String getSetting(String projectId, String name) {
        var id = idConvertor.revert(projectId);
        return panelSettingMapper.get(id, name);
    }
}
