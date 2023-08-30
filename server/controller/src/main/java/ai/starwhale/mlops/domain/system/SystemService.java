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

import ai.starwhale.mlops.api.protocol.system.FeaturesVo;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final SystemSettingService systemSettingService;


    private final String controllerVersion;
    private final FeaturesProperties featuresProperties;

    public SystemService(SystemSettingService systemSettingService,
                         @Value("${sw.version}") String controllerVersion,
                         FeaturesProperties featuresProperties) {
        this.systemSettingService = systemSettingService;
        this.controllerVersion = controllerVersion;
        this.featuresProperties = featuresProperties;
    }

    public String controllerVersion() {
        return controllerVersion;
    }

    public List<ResourcePool> listResourcePools() {
        return systemSettingService.getResourcePoolsFromWeb();
    }

    public void updateResourcePools(List<ResourcePool> resourcePools) {
        systemSettingService.updateResourcePools(resourcePools);
    }

    public FeaturesVo queryFeatures() {
        return FeaturesVo.builder()
                .disabled(featuresProperties.getDisabled())
                .build();
    }
}
