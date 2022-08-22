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

package ai.starwhale.mlops.domain.system.resourcepool;

import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ResourcePoolCache implements CommandLineRunner {

    final ResourcePoolMapper resourcePoolMapper;

    final Map<String, ResourcePoolEntity> resourcePools;

    public ResourcePoolCache(ResourcePoolMapper resourcePoolMapper) {
        this.resourcePoolMapper = resourcePoolMapper;
        resourcePools = new ConcurrentHashMap<>();
    }

    public void labelReport(List<ResourcePool> resourcePools) {
        resourcePools.forEach(resourcePool -> {
            this.resourcePools.computeIfAbsent(resourcePool.getLabel(), k -> {
                var entity = ResourcePoolEntity.builder().label(resourcePool.getLabel()).build();
                var id = resourcePoolMapper.add(entity);
                entity.setId(id);
                return entity;
            });
        });
    }

    @Override
    public void run(String... args) throws Exception {
        resourcePoolMapper.listResourcePools().forEach(item -> resourcePools.put(item.getLabel(), item));
    }
}
