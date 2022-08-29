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
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ResourcePoolManager {
    @Resource
    private ResourcePoolMapper resourcePoolMapper;

    public Long getResourcePoolId(@NotNull String label) {
        if(!StringUtils.hasText(label)){
            label = ResourcePool.DEFAULT;
        }
        var entity = resourcePoolMapper.findByLabel(label);
        if (entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RESOURCE_POOL)
                .tip(String.format("Unable to find resource pool %s", label)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
