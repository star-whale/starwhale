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

import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.panel.bo.PanelPlugin;
import ai.starwhale.mlops.domain.panel.mapper.PanelPluginMapper;
import ai.starwhale.mlops.domain.panel.po.PanelPluginEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
public class PluginService {

    private final StorageAccessService storageAccessService;

    private final StoragePathCoordinator storagePathCoordinator;

    private final PanelPluginMapper panelPluginMapper;

    private final PanelPluginConvertor panelPluginConvertor;

    private final IdConvertor idConvertor;

    private final ObjectMapper yamlMapper;

    PluginService(
            StorageAccessService storageAccessService,
            StoragePathCoordinator storagePathCoordinator,
            @Qualifier("yamlMapper") ObjectMapper yamlMapper,
            PanelPluginMapper panelPluginMapper,
            PanelPluginConvertor panelPluginConvertor,
            IdConvertor idConvertor
    ) {
        this.storageAccessService = storageAccessService;
        this.storagePathCoordinator = storagePathCoordinator;
        this.yamlMapper = yamlMapper;
        this.panelPluginMapper = panelPluginMapper;
        this.panelPluginConvertor = panelPluginConvertor;
        this.idConvertor = idConvertor;
    }

    public void installPlugin(MultipartFile multipartFile) {
        byte[] content;
        PanelPlugin.PluginManifest manifest;
        // 1.save to oss
        try (var input = multipartFile.getInputStream(); var dupIs = multipartFile.getInputStream()) {
            content = TarFileUtil.getContentFromTarFile(dupIs, "", "manifest.json");
            // check manifest
            if (content == null) {
                throw new StarwhaleApiException(new SwProcessException(SwProcessException.ErrorType.SYSTEM),
                    HttpStatus.BAD_REQUEST);
            }
            manifest = yamlMapper.readValue(content, PanelPlugin.PluginManifest.class);

            // check if exists
            var plugin = panelPluginMapper.getByNameAndVersion(manifest.name, manifest.version);
            if (plugin != null) {
                throw new StarwhaleApiException(new SwProcessException(SwProcessException.ErrorType.DB),
                    HttpStatus.CONFLICT);
            }

            var path = storagePathCoordinator.allocatePluginPath(manifest.name, manifest.version);
            storageAccessService.put(path, input, multipartFile.getSize());
        } catch (IOException e) {
            throw new StarwhaleApiException(new SwProcessException(SwProcessException.ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 2.update db
        var entity = PanelPluginEntity.builder()
                .name(manifest.name)
                .version(manifest.version)
                .meta(new String(content, StandardCharsets.UTF_8))
                .build();
        panelPluginMapper.add(entity);

        // TODO make cache in static resource path
    }

    public void uninstallPlugin(String idStr) {
        var id = idConvertor.revert(idStr);
        panelPluginMapper.remove(id);
        // leave the plugin tarball in oss

        // TODO remove cache from static resource path
    }

    public PageInfo<PanelPluginVo> listPlugin() {
        return PageUtil.toPageInfo(panelPluginMapper.list(), panelPluginConvertor::convert);
    }
}
