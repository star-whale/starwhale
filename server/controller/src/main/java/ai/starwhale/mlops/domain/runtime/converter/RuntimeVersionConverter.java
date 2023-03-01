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

package ai.starwhale.mlops.domain.runtime.converter;

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RuntimeVersionConverter {

    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;

    public RuntimeVersionConverter(IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor) {
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public RuntimeVersionVo convert(RuntimeVersionEntity entity, RuntimeVersionEntity latest)
            throws ConvertException {
        if (entity == null) {
            return null;
        }
        RuntimeVersionVo vo = RuntimeVersionVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getVersionName())
                .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                .tag(entity.getVersionTag())
                .meta(entity.getVersionMeta())
                .image(entity.getImage())
                .createdTime(entity.getCreatedTime().getTime())
                .runtimeId(idConvertor.convert(entity.getRuntimeId()))
                .build();
        if (latest != null && Objects.equals(entity.getId(), latest.getId())) {
            vo.setAlias(VersionAliasConverter.LATEST);
        }
        return vo;
    }

}
