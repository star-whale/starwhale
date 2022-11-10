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

package ai.starwhale.mlops.domain.panel.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.panel.po.PanelPluginEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;


@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class PanelPluginMapperTest extends MySqlContainerHolder {
    @Autowired
    private PanelPluginMapper panelPluginMapper;

    private PanelPluginEntity entity;

    private final String pluginNameExists = "foo";
    private final String pluginNameDeleted = "bar";
    private final String pluginNameNonExist = "baz";
    private final String versionExists = "v1";
    private final String versionNonExist = "v2";
    private final String storagePath = "/foo";

    @BeforeEach
    public void setUp() {
        entity = PanelPluginEntity.builder()
                .name(pluginNameExists)
                .version(versionExists)
                .meta("{}")
                .storagePath(storagePath)
                .build();
        panelPluginMapper.add(entity);
        var deleted = PanelPluginEntity.builder()
                .name(pluginNameDeleted)
                .version(versionExists)
                .meta("{}")
                .storagePath(storagePath)
                .build();
        panelPluginMapper.add(deleted);
        panelPluginMapper.remove(deleted.getId());
    }

    @Test
    public void testListJobs() {
        var plugins = panelPluginMapper.list();
        Assertions.assertEquals(1, plugins.size());
        Assertions.assertEquals(entity, plugins.get(0));
        Assertions.assertEquals(storagePath, plugins.get(0).getStoragePath());
    }

    @Test
    public void testGetByName() {
        var plugins = panelPluginMapper.get(pluginNameExists);
        Assertions.assertEquals(1, plugins.size());
        Assertions.assertEquals(entity, plugins.get(0));
        Assertions.assertEquals(storagePath, plugins.get(0).getStoragePath());

        List.of(pluginNameNonExist, pluginNameDeleted).forEach(name -> {
            var resp = panelPluginMapper.get(name);
            Assertions.assertEquals(0, resp.size());
        });
    }

    @Test
    public void testGetByNameAndVersion() {
        var plugin = panelPluginMapper.getByNameAndVersion(pluginNameExists, versionExists);
        Assertions.assertEquals(entity, plugin);
        Assertions.assertEquals(storagePath, plugin.getStoragePath());
        Assertions.assertNull(panelPluginMapper.getByNameAndVersion(pluginNameExists, versionNonExist));

        List.of(pluginNameNonExist, pluginNameDeleted).forEach(name -> {
            Assertions.assertNull(panelPluginMapper.getByNameAndVersion(name, versionExists));
            Assertions.assertNull(panelPluginMapper.getByNameAndVersion(name, versionNonExist));
        });
    }
}
