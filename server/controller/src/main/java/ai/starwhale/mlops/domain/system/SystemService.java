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

import ai.starwhale.mlops.api.protocol.system.ResourcePoolVo;
import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolConverter;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final ResourcePoolMapper resourcePoolMapper;

    private final ResourcePoolConverter resourcePoolConverter;

    private final String controllerVersion;

    private final StorageAccessService storageAccessService;

    public SystemService(ResourcePoolMapper resourcePoolMapper,
            ResourcePoolConverter resourcePoolConverter, @Value("${sw.version}") String controllerVersion,
            StorageAccessService storageAccessService) {
        this.resourcePoolMapper = resourcePoolMapper;
        this.resourcePoolConverter = resourcePoolConverter;
        this.controllerVersion = controllerVersion;
        this.storageAccessService = storageAccessService;
    }

    public String controllerVersion() {
        return controllerVersion;
    }

    public List<ResourcePoolVo> listResourcePools() {
        var entities = resourcePoolMapper.listResourcePools();
        return entities.stream().map(resourcePoolConverter::toResourcePoolVo).collect(Collectors.toList());
    }

}
