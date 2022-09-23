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

package ai.starwhale.mlops.domain.runtime;

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RuntimeConvertor implements Convertor<RuntimeEntity, RuntimeVo> {

    @Resource
    private IdConvertor idConvertor;
    @Resource
    private UserConvertor userConvertor;

    @Override
    public RuntimeVo convert(RuntimeEntity entity) throws ConvertException {
        return RuntimeVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getRuntimeName())
                .owner(userConvertor.convert(entity.getOwner()))
                .createdTime(entity.getCreatedTime().getTime())
                .build();
    }

    @Override
    public RuntimeEntity revert(RuntimeVo runtimeVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
