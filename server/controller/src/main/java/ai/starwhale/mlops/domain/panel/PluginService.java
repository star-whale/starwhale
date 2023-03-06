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
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.panel.bo.PanelPlugin;
import ai.starwhale.mlops.domain.panel.mapper.PanelPluginMapper;
import ai.starwhale.mlops.domain.panel.po.PanelPluginEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.github.pagehelper.PageInfo;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
public class PluginService implements CommandLineRunner {

    private final StorageAccessService storageAccessService;

    private final StoragePathCoordinator storagePathCoordinator;

    private final PanelPluginMapper panelPluginMapper;

    private final PanelPluginConverter panelPluginConvertor;

    private final IdConverter idConvertor;

    private final String cachePath;

    PluginService(
            StorageAccessService storageAccessService,
            StoragePathCoordinator storagePathCoordinator,
            PanelPluginMapper panelPluginMapper,
            PanelPluginConverter panelPluginConvertor,
            IdConverter idConvertor,
            @Value("${spring.web.resources.static-locations}") String[] staticLocations
    ) {
        this.storageAccessService = storageAccessService;
        this.storagePathCoordinator = storagePathCoordinator;
        this.panelPluginMapper = panelPluginMapper;
        this.panelPluginConvertor = panelPluginConvertor;
        this.idConvertor = idConvertor;
        // find the first local filesystem location
        var path = Arrays.stream(staticLocations).filter(i -> i.startsWith("file:")).findFirst().orElse("");
        this.cachePath = path.replaceFirst("^file:", "");
        log.debug("use {} as plugin cache path", this.cachePath);
    }

    public void installPlugin(MultipartFile multipartFile) {
        byte[] content;
        String storagePath;
        PanelPlugin.PluginManifest manifest;
        // 1.save to oss
        try (var input = multipartFile.getInputStream(); var dupIs = multipartFile.getInputStream()) {
            content = TarFileUtil.getContentFromTarFile(dupIs, "", "manifest.json");
            // check manifest
            if (content == null) {
                throw new SwValidationException(ValidSubject.PLUGIN, "manifest is empty");
            }
            manifest = Constants.yamlMapper.readValue(content, PanelPlugin.PluginManifest.class);

            // check if exists
            var plugin = panelPluginMapper.getByNameAndVersion(manifest.name, manifest.version);
            if (plugin != null) {
                throw new SwValidationException(ValidSubject.PLUGIN, "plugin exists");
            }

            storagePath = storagePathCoordinator.allocatePluginPath(manifest.name, manifest.version);
            storageAccessService.put(storagePath, input, multipartFile.getSize());
        } catch (IOException e) {
            log.error("save plugin tarball to storage fail", e);
            throw new SwProcessException(SwProcessException.ErrorType.STORAGE);
        }

        // 2. make cache in local filesystem
        try (var tar = multipartFile.getInputStream()) {
            initPluginCache(tar, manifest.name);
        } catch (IOException | ArchiveException e) {
            throw new SwProcessException(SwProcessException.ErrorType.SYSTEM, "can not extract plugin", e);
        }

        // 3.update db
        var entity = PanelPluginEntity.builder()
                .name(manifest.name)
                .version(manifest.version)
                .meta(new String(content, StandardCharsets.UTF_8))
                .storagePath(storagePath)
                .build();
        panelPluginMapper.add(entity);
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

    private void initPluginCache(InputStream is, String subPath) throws IOException, ArchiveException {
        if (cachePath.isEmpty()) {
            log.warn("cache path is empty");
            return;
        }
        TarFileUtil.extract(is, Paths.get(cachePath, subPath).toString());
    }

    private void initPluginCaches() {
        panelPluginMapper.list().forEach(plugin -> {
            try {
                var is = Retry.decorateCheckedSupplier(
                        Retry.of("get plugin", RetryConfig.custom()
                                .maxAttempts(3)
                                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                                .build()),
                        () -> this.storageAccessService.get(plugin.getStoragePath())).apply();
                initPluginCache(is, plugin.getName());
            } catch (Throwable e) {
                log.error("can not init plugin {}@{}, err {}", plugin.getName(), plugin.getVersion(), e);
            }
        });
    }

    @Override
    public void run(String... args) throws Exception {
        initPluginCaches();
    }
}
