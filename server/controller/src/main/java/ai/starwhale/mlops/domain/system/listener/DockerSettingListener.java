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

package ai.starwhale.mlops.domain.system.listener;

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.domain.system.SystemSetting;
import ai.starwhale.mlops.domain.system.SystemSettingListener;
import org.springframework.stereotype.Component;

@Component
public class DockerSettingListener implements SystemSettingListener {

    private final DockerSetting dockerSetting;

    public DockerSettingListener(DockerSetting dockerSetting) {
        this.dockerSetting = dockerSetting;
    }

    @Override
    public void onUpdate(SystemSetting systemSetting) {
        this.dockerSetting.setRegistryForPull(systemSetting.getDockerSetting().getRegistryForPull());
        this.dockerSetting.setRegistryForPush(systemSetting.getDockerSetting().getRegistryForPush());
        this.dockerSetting.setInsecure(systemSetting.getDockerSetting().isInsecure());
        this.dockerSetting.setUserName(systemSetting.getDockerSetting().getUserName());
        this.dockerSetting.setPassword(systemSetting.getDockerSetting().getPassword());
    }
}
