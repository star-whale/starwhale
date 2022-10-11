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
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.system.mapper.SystemSettingMapper;
import ai.starwhale.mlops.domain.system.po.SystemSettingEntity;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemSettingServiceTest {

    private SystemSettingService systemSettingService;
    SystemSettingMapper systemSettingMapper;

    @BeforeEach
    public void setUp() throws Exception {
        systemSettingMapper = mock(SystemSettingMapper.class);
        when(systemSettingMapper.get()).thenReturn(new SystemSettingEntity(1L, YAML));
        systemSettingService = new SystemSettingService(new YAMLMapper(), systemSettingMapper);
        systemSettingService.run();
    }

    static String YAML = "---\n"
            + "dockerSetting:\n"
            + "  registry: \"abcd.com\"";

    static String YAML2 = "---\n"
            + "dockerSetting:\n"
            + "  registry: \"abcd1.com\"";

    @Test
    public void testAppStartWithSetting() {
        Assertions.assertEquals("abcd.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistry());

    }

    @Test
    public void testAppStartWithoutSetting() {
        when(systemSettingMapper.get()).thenReturn(null);
        systemSettingService = new SystemSettingService(new YAMLMapper(), systemSettingMapper);
        Assertions.assertNull(systemSettingService.getSystemSetting());

    }

    @Test
    public void testUpdate() {
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals("abcd1.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
    }

    @Test
    public void testUpdateWitouData() {
        when(systemSettingMapper.get()).thenReturn(null);
        systemSettingService = new SystemSettingService(new YAMLMapper(), systemSettingMapper);
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals("abcd1.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
    }

    @Test
    public void testQueryWithData() {
        Assertions.assertEquals(YAML, systemSettingService.querySetting().trim());
    }

    @Test
    public void testQueryWithoutData() {
        when(systemSettingMapper.get()).thenReturn(null);
        systemSettingService = new SystemSettingService(new YAMLMapper(), systemSettingMapper);
        Assertions.assertEquals("", systemSettingService.querySetting());
    }


}
