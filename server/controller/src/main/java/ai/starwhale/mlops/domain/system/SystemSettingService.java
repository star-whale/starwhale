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

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.domain.system.mapper.SystemSettingMapper;
import ai.starwhale.mlops.domain.system.po.SystemSettingEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Order(1)
@Service
public class SystemSettingService implements CommandLineRunner {

    private final YAMLMapper yamlMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final List<SystemSettingListener> listeners;
    private final RunTimeProperties runTimeProperties;
    private final DockerSetting dockerSetting;
    @Getter
    protected SystemSetting systemSetting;

    public SystemSettingService(YAMLMapper yamlMapper,
            SystemSettingMapper systemSettingMapper, List<SystemSettingListener> listeners,
            RunTimeProperties runTimeProperties, DockerSetting dockerSetting) {
        this.yamlMapper = yamlMapper;
        this.systemSettingMapper = systemSettingMapper;
        this.listeners = listeners;
        this.runTimeProperties = runTimeProperties;
        this.dockerSetting = dockerSetting;
    }

    public String querySetting() {
        try {
            return yamlMapper.writeValueAsString(systemSetting);
        } catch (JsonProcessingException e) {
            log.error("write systemSetting setting to yaml failed", e);
            throw new SwProcessException(ErrorType.SYSTEM);
        }
    }

    public String updateSetting(String setting) {
        try {
            systemSetting = yamlMapper.readValue(setting, SystemSetting.class);
            if (null == systemSetting.getPypiSetting()) {
                systemSetting.setPypiSetting(Pypi.empty());
            }
            if (null == systemSetting.getDockerSetting()) {
                systemSetting.setDockerSetting(DockerSetting.empty());
            }
            setting = yamlMapper.writeValueAsString(systemSetting);
        } catch (JsonProcessingException e) {
            log.error("invalid setting yaml {}", setting, e);
            throw new SwValidationException(ValidSubject.SETTING);
        }
        systemSettingMapper.put(setting);
        listeners.forEach(l -> l.onUpdate(systemSetting));
        return querySetting();
    }

    public ResourcePool queryResourcePool(String rpName) {
        return CollectionUtils.isEmpty(this.systemSetting.getResourcePoolSetting()) ? ResourcePool.defaults() :
                this.systemSetting.getResourcePoolSetting().stream().filter(rp -> rp.getName().equals(rpName)).findAny()
                        .orElse(ResourcePool.defaults());
    }

    public List<ResourcePool> getResourcePools() {
        return CollectionUtils.isEmpty(this.systemSetting.getResourcePoolSetting()) ? List.of(ResourcePool.defaults())
                : this.systemSetting.getResourcePoolSetting();
    }


    @Override
    public void run(String... args) throws Exception {
        SystemSettingEntity setting = systemSettingMapper.get();
        if (null != setting) {
            try {
                systemSetting = yamlMapper.readValue(setting.getContent(), SystemSetting.class);
                if (null == systemSetting.getPypiSetting()) {
                    systemSetting.setPypiSetting(runTimeProperties.getPypi());
                }
                if (null == systemSetting.getDockerSetting()) {
                    systemSetting.setDockerSetting(dockerSetting);
                }
                listeners.forEach(l -> l.onUpdate(systemSetting));
            } catch (JsonProcessingException e) {
                log.error("corrupted system setting yaml");
                throw new SwValidationException(ValidSubject.SETTING);
            }
        } else {
            systemSetting = new SystemSetting();
            systemSetting.setPypiSetting(runTimeProperties.getPypi());
            systemSetting.setDockerSetting(dockerSetting);
        }

    }
}
