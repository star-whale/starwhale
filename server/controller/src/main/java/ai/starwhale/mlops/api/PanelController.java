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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import ai.starwhale.mlops.domain.panel.PanelSettingService;
import ai.starwhale.mlops.domain.panel.PluginService;
import com.github.pagehelper.PageInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class PanelController implements PanelApi {

    private final PluginService pluginService;
    private final PanelSettingService panelSettingService;

    PanelController(PluginService pluginService, PanelSettingService panelSettingService) {
        this.pluginService = pluginService;
        this.panelSettingService = panelSettingService;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> installPlugin(MultipartFile file) {
        pluginService.installPlugin(file);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> uninstallPlugin(String id) {
        pluginService.uninstallPlugin(id);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<PanelPluginVo>>> pluginList() {
        // TODO support page params
        return ResponseEntity.ok(Code.success.asResponse(pluginService.listPlugin()));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> getPanelSetting(String projectId, String key) {
        return ResponseEntity.ok(Code.success.asResponse(panelSettingService.getSetting(projectId, key)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> setPanelSetting(String projectId, String key, String content) {
        panelSettingService.saveSetting(projectId, key, content);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }
}
