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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import ai.starwhale.mlops.domain.panel.PanelSettingService;
import ai.starwhale.mlops.domain.panel.PluginService;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

public class PanelControllerTest {

    private PanelController controller;
    private PanelSettingService panelSettingService;
    private PanelPluginVo existPlugin;

    @BeforeEach
    public void setUp() {
        var pluginService = mock(PluginService.class);
        panelSettingService = mock(PanelSettingService.class);
        controller = new PanelController(pluginService, panelSettingService);
        existPlugin = PanelPluginVo.builder().name("foo").version("bar").build();
        given(pluginService.listPlugin()).willReturn(
            new PageInfo<>(List.of(existPlugin)));
    }

    @Test
    public void testListPlugins() {
        var resp = controller.pluginList();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                is(hasProperty("pageNum", is(1))),
                is(hasProperty("pageSize", is(1))),
                is(hasProperty("list", is(List.of(existPlugin))))
        ));
    }

    @Test
    public void testInstallPlugin() {
        var file = mock(MultipartFile.class);
        var resp = controller.installPlugin(file);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testUninstallPlugin() {
        var resp = controller.uninstallPlugin("foo");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetPanelSetting() {
        var content = "foobar";
        given(panelSettingService.getSetting(anyString(), anyString())).willReturn(content);
        var resp = controller.getPanelSetting("1", "key");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is(content));
    }

    @Test
    public void testSetPanelSetting() {
        var resp = controller.setPanelSetting("1", "key", "val");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }
}
