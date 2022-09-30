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

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemSettingServiceTest {

    private SystemSettingService systemSettingService;
    StorageAccessService storageAccessService;

    @BeforeEach
    public void setUp() throws Exception {
        storageAccessService = mock(StorageAccessService.class);
        StoragePathCoordinator storagePathCoordinator = new StoragePathCoordinator("test");
        String path = storagePathCoordinator.allocateSystemSettingPath(SystemSettingService.PATH_SETTING);
        when(storageAccessService.head(path)).thenReturn(new StorageObjectInfo(true, 1L, ""));
        when(storageAccessService.get(path)).thenReturn(
                new LengthAbleInputStream(new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)), 1L));
        systemSettingService = new SystemSettingService(new YAMLMapper(), storagePathCoordinator,
                storageAccessService);
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
    public void testAppStartWithoutSetting() throws IOException {
        when(storageAccessService.head(systemSettingService.path)).thenReturn(new StorageObjectInfo(false, null, ""));
        systemSettingService = new SystemSettingService(new YAMLMapper(), new StoragePathCoordinator("test"),
                storageAccessService);
        Assertions.assertNull(systemSettingService.getSystemSetting());

    }

    @Test
    public void testUpdate() {
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals("abcd1.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
    }

    @Test
    public void testUpdateWitouData() throws IOException {
        when(storageAccessService.head(systemSettingService.path)).thenReturn(new StorageObjectInfo(false, null, ""));
        systemSettingService = new SystemSettingService(new YAMLMapper(), new StoragePathCoordinator("test"),
                storageAccessService);
        systemSettingService.updateSetting(YAML2);
        Assertions.assertEquals("abcd1.com",
                systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
    }

    @Test
    public void testQueryWithData() {
        Assertions.assertEquals(YAML, systemSettingService.querySetting().trim());
    }

    @Test
    public void testQueryWithoutData() throws IOException {
        when(storageAccessService.head(systemSettingService.path)).thenReturn(new StorageObjectInfo(false, null, ""));
        systemSettingService = new SystemSettingService(new YAMLMapper(), new StoragePathCoordinator("test"),
                storageAccessService);
        Assertions.assertEquals("", systemSettingService.querySetting());
    }


}
