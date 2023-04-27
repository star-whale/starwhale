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

import ai.starwhale.mlops.api.protocol.system.LatestVersionVo;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.upgrade.UpgradeService;
import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import ai.starwhale.mlops.domain.upgrade.bo.Version;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final SystemSettingService systemSettingService;

    private final UpgradeService upgradeService;
    private final String controllerVersion;

    public SystemService(SystemSettingService systemSettingService,
            UpgradeService upgradeService,
            @Value("${sw.version}") String controllerVersion) {
        this.systemSettingService = systemSettingService;
        this.upgradeService = upgradeService;
        this.controllerVersion = controllerVersion;
    }

    public String upgrade(String version, String image) {
        return upgradeService.upgrade(new Version(version, image)).getProgressId();
    }

    public void cancelUpgrading() {
        upgradeService.cancelUpgrade();
    }

    public List<UpgradeLog> getUpgradeLog() {
        return upgradeService.getUpgradeLog();
    }

    public String controllerVersion() {
        return controllerVersion;
    }

    public LatestVersionVo getLatestVersion() {
        Version latestVersion = upgradeService.getLatestVersion();
        return LatestVersionVo.builder()
                .version(latestVersion.getNumber())
                .image(latestVersion.getImage())
                .build();
    }

    public List<ResourcePool> listResourcePools() {
        return systemSettingService.getResourcePools();
    }

    public void updateResourcePools(List<ResourcePool> resourcePools) {
        systemSettingService.updateResourcePools(resourcePools);
    }
}
