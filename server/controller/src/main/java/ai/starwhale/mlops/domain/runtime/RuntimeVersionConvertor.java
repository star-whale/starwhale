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

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RuntimeVersionConvertor implements Convertor<RuntimeVersionEntity, RuntimeVersionVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public RuntimeVersionVO convert(RuntimeVersionEntity entity)
        throws ConvertException {
        return RuntimeVersionVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .name(entity.getVersionName())
            .owner(userConvertor.convert(entity.getOwner()))
            .tag(entity.getVersionTag())
            .meta(entity.getVersionMeta())
            .manifest(entity.getManifest())
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .build();
    }

    @Override
    public RuntimeVersionEntity revert(RuntimeVersionVO runtimeVersionVO) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
