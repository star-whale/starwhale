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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.domain.system.mapper.SystemSettingMapper;
import ai.starwhale.mlops.domain.system.po.SystemSettingEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemSettingServiceTest {

    static String YAML = "---\n"
            + "dockerSetting:\n"
            + "  registryForPull: \"abcd.com\"\n"
            + "  registryForPush: \"abcd2.com\"\n"
            + "  userName: \"guest\"\n"
            + "  password: \"guest123\"\n"
            + "  insecure: false\n"
            + "pypiSetting:\n"
            + "  indexUrl: \"url1\"\n"
            + "  extraIndexUrl: \"url2\"\n"
            + "  trustedHost: \"host1\"\n"
            + "  retries: 11\n"
            + "  timeout: 91\n"
            + "resourcePoolSetting:\n"
            + "- name: \"default\"\n"
            + "  nodeSelector: {}\n"
            + "  resources:\n"
            + "  - name: \"cpu\"\n"
            + "    max: null\n"
            + "    min: null\n"
            + "    defaults: null\n"
            + "  - name: \"memory\"\n"
            + "    max: null\n"
            + "    min: null\n"
            + "    defaults: null\n"
            + "  tolerations: null\n"
            + "  metadata: null\n"
            + "  isPrivate: null\n"
            + "  visibleUserIds: null";
    static String YAML2 = "---\n"
            + "dockerSetting:\n"
            + "  registryForPull: \"abcd1.com\"\n"
            + "  registryForPush: \"abcd2.com\"\n"
            + "  userName: \"admin\"\n"
            + "  password: \"admin123\"\n"
            + "  insecure: \"false\"\n"
            + "resourcePoolSetting:\n"
            + "- name: \"custom\"\n"
            + "  nodeSelector: {}\n"
            + "  resources:\n"
            + "  - name: \"cpu\"\n"
            + "  - name: \"memory\"\n"
            + "  - name: \"nvidia.com/gpu\"";
    SystemSettingMapper systemSettingMapper;
    SystemSettingListener listener;
    private SystemSettingService systemSettingService;

    @BeforeEach
    public void setUp() throws Exception {
        systemSettingMapper = mock(SystemSettingMapper.class);
        when(systemSettingMapper.get()).thenReturn(new SystemSettingEntity(1L, YAML));
        listener = mock(SystemSettingListener.class);
        var userService = mock(UserService.class);
        when(userService.currentUserDetail()).thenReturn(User.builder().id(2L).build());
        systemSettingService = new SystemSettingService(
                systemSettingMapper,
                List.of(listener),
                new RunTimeProperties("", new RunTimeProperties.ImageBuild(),
                        new Pypi("url1", "url2", "host1", 11, 91)),
                new DockerSetting("", "", "", "", false),
                userService);
        systemSettingService.run();
    }

    @Test
    public void testAppStartWithSetting() {
        Assertions.assertEquals("abcd.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPull());
        verify(listener).onUpdate(systemSettingService.getSystemSetting());

    }

    @Test
    public void testUpdate() {
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals("abcd1.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPull());
        Assertions.assertEquals("abcd2.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPush());
        Assertions.assertEquals("admin",
                systemSettingService.getSystemSetting().getDockerSetting().getUserName());
        Assertions.assertEquals("admin123",
                systemSettingService.getSystemSetting().getDockerSetting().getPassword());
        Assertions.assertFalse(systemSettingService.getSystemSetting().getDockerSetting().isInsecure());
        // get the custom resource pool
        Assertions.assertEquals(1, systemSettingService.getResourcePools().size());
        Assertions.assertEquals(3, systemSettingService.queryResourcePool("custom").getResources().size());
        // get the default resource pool
        Assertions.assertEquals(ResourcePool.defaults(), systemSettingService.queryResourcePool("not_exists"));
        Assertions.assertEquals(2, systemSettingService.queryResourcePool("not_exists").getResources().size());
        verify(listener).onUpdate(systemSettingService.getSystemSetting());
    }

    @Test
    public void testUpdateWithData() {
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals("abcd1.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPull());
        verify(listener).onUpdate(systemSettingService.getSystemSetting());
    }

    @Test
    public void testUnsetSetting() {
        systemSettingService.updateSetting("--- {}");
        Assertions.assertEquals("---\n"
                + "dockerSetting:\n"
                + "  registryForPull: \"\"\n"
                + "  registryForPush: \"\"\n"
                + "  userName: \"\"\n"
                + "  password: \"\"\n"
                + "  insecure: true\n"
                + "pypiSetting:\n"
                + "  indexUrl: \"\"\n"
                + "  extraIndexUrl: \"\"\n"
                + "  trustedHost: \"\"\n"
                + "  retries: 10\n"
                + "  timeout: 90\n"
                + "resourcePoolSetting:\n"
                + "- name: \"default\"\n"
                + "  nodeSelector: {}\n"
                + "  resources:\n"
                + "  - name: \"cpu\"\n"
                + "    max: null\n"
                + "    min: null\n"
                + "    defaults: null\n"
                + "  - name: \"memory\"\n"
                + "    max: null\n"
                + "    min: null\n"
                + "    defaults: null\n"
                + "  tolerations: null\n"
                + "  metadata: null\n"
                + "  isPrivate: null\n"
                + "  visibleUserIds: null",
                systemSettingService.querySetting().trim());
        ResourcePool resourcePool = systemSettingService.queryResourcePool("abc");
        Assertions.assertEquals(ResourcePool.defaults().getName(), resourcePool.getName());
    }

    @Test
    public void testQueryWithData() {
        Assertions.assertEquals(YAML, systemSettingService.querySetting().trim());
    }

    @Test
    public void testStartWithoutData() throws Exception {
        SystemSettingService systemSettingService = new SystemSettingService(
                mock(SystemSettingMapper.class),
                List.of(listener),
                new RunTimeProperties("", new RunTimeProperties.ImageBuild(), new Pypi("", "", "", 1, 2)),
                new DockerSetting("abcd.com", "abcd2.com", "admin", "admin123", false),
                mock(UserService.class));
        systemSettingService.run();
        Assertions.assertEquals("---\n"
                + "dockerSetting:\n"
                + "  registryForPull: \"abcd.com\"\n"
                + "  registryForPush: \"abcd2.com\"\n"
                + "  userName: \"admin\"\n"
                + "  password: \"admin123\"\n"
                + "  insecure: false\n"
                + "pypiSetting:\n"
                + "  indexUrl: \"\"\n"
                + "  extraIndexUrl: \"\"\n"
                + "  trustedHost: \"\"\n"
                + "  retries: 1\n"
                + "  timeout: 2\n"
                + "resourcePoolSetting:\n"
                + "- name: \"default\"\n"
                + "  nodeSelector: {}\n"
                + "  resources:\n"
                + "  - name: \"cpu\"\n"
                + "    max: null\n"
                + "    min: null\n"
                + "    defaults: null\n"
                + "  - name: \"memory\"\n"
                + "    max: null\n"
                + "    min: null\n"
                + "    defaults: null\n"
                + "  tolerations: null\n"
                + "  metadata: null\n"
                + "  isPrivate: null\n"
                + "  visibleUserIds: null",
                systemSettingService.querySetting().trim());
        ResourcePool resourcePool = systemSettingService.queryResourcePool("abc");
        Assertions.assertEquals(ResourcePool.defaults().getName(), resourcePool.getName());
    }

    @Test
    public void testUpdateResourcePools() {
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals(1, systemSettingService.getResourcePools().size());
        verify(listener).onUpdate(systemSettingService.getSystemSetting());

        var pool = ResourcePool.builder().name("foo").metadata(Map.of("bar", "baz")).build();
        systemSettingService.updateResourcePools(List.of(pool));
        Assertions.assertEquals(1, systemSettingService.getResourcePools().size());
        Assertions.assertEquals(pool, systemSettingService.queryResourcePool("foo"));
        verify(listener, times(2)).onUpdate(systemSettingService.getSystemSetting());
    }

    @Test
    public void testPrivateResourcePool() {
        // private pool should not be visible to any users
        var pool = ResourcePool.builder().name("foo").isPrivate(true).build();
        systemSettingService.updateResourcePools(List.of(pool));
        Assertions.assertEquals(0, systemSettingService.getResourcePools().size());

        // update pool to be visible to user 2
        pool.setVisibleUserIds(List.of(2L));
        systemSettingService.updateResourcePools(List.of(pool));
        Assertions.assertEquals(1, systemSettingService.getResourcePools().size());
        Assertions.assertEquals(pool, systemSettingService.queryResourcePool("foo"));

        // get the rendered yaml, should contain visibleUserIds
        var yaml = systemSettingService.querySetting();
        Assertions.assertTrue(yaml.contains("visibleUserIds:\n  - 2"));
    }
}
