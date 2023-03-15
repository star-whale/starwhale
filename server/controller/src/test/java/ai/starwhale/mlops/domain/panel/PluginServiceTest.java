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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.domain.panel.mapper.PanelPluginMapper;
import ai.starwhale.mlops.domain.panel.po.PanelPluginEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

public class PluginServiceTest {

    private PluginService service;
    private PanelPluginMapper panelPluginMapper;
    private StorageAccessService storageAccessService;

    @BeforeEach
    public void setUp() {
        storageAccessService = mock(StorageAccessService.class);
        var storagePathCoordinator = new StoragePathCoordinator("/root");
        panelPluginMapper = mock(PanelPluginMapper.class);
        var idConvertor = new IdConverter();
        var panelPluginConvertor = new PanelPluginConverter(idConvertor);
        var locations = new String[]{"file:/opt/starwhale.static"};
        service = new PluginService(
                storageAccessService,
                storagePathCoordinator,
                panelPluginMapper,
                panelPluginConvertor,
                idConvertor,
                locations
        );
    }

    @Test
    public void testListPlugins() {
        var name = "foo";
        var version = "bar";
        var plugin = PanelPluginEntity.builder()
                .name(name)
                .version(version)
                .build();

        given(panelPluginMapper.list()).willReturn(List.of(plugin));
        var resp = service.listPlugin();
        assertThat(resp.getList(), is(List.of(PanelPluginVo.builder().name(name).version(version).build())));
    }

    @Test
    public void testInstallPlugin() {
        var manifest = "{\"name\": \"foo\", \"version\": \"bar\"}";
        var plugInContent = new byte[] {1, 2, 3};
        given(panelPluginMapper.getByNameAndVersion(anyString(), anyString())).willReturn(null);
        try (var tar = mockStatic(TarFileUtil.class)) {
            tar.when(() -> TarFileUtil.getContentFromTarFile(any(), anyString(), anyString()))
                    .thenReturn(manifest.getBytes(StandardCharsets.UTF_8));
            var file = new MockMultipartFile("plugin", plugInContent);
            service.installPlugin(file);

            verify(storageAccessService).put(anyString(), any(InputStream.class),
                    eq(Long.valueOf(plugInContent.length)));
            verify(panelPluginMapper).getByNameAndVersion("foo", "bar");
            var expected = PanelPluginEntity.builder().name("foo").version("bar").meta(manifest).build();
            verify(panelPluginMapper).add(expected);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testUninstallPlugin() {
        var id = "42";
        service.uninstallPlugin(id);
        verify(panelPluginMapper).remove(42L);
    }

    @Test
    public void testRun() throws Exception {
        var plugin = PanelPluginEntity.builder()
                .name("foo")
                .version("bar")
                .build();

        given(panelPluginMapper.list()).willReturn(List.of(plugin));
        var resp = new byte[] {1, 2, 3};
        var ossResp = new LengthAbleInputStream(new ByteArrayInputStream(resp), resp.length);
        given(storageAccessService.get(anyString())).willReturn(ossResp);

        try (var tar = mockStatic(TarFileUtil.class)) {
            service.run();
        }
    }
}
