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

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class RuntimeVersionConvertor implements Convertor<RuntimeVersionEntity, RuntimeVersionVo> {

    private final IdConvertor idConvertor;
    private final UserConvertor userConvertor;
    private final LocalDateTimeConvertor localDateTimeConvertor;
    private final VersionAliasConvertor versionAliasConvertor;

    public RuntimeVersionConvertor(IdConvertor idConvertor, UserConvertor userConvertor,
            LocalDateTimeConvertor localDateTimeConvertor, VersionAliasConvertor versionAliasConvertor) {
        this.idConvertor = idConvertor;
        this.userConvertor = userConvertor;
        this.localDateTimeConvertor = localDateTimeConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    @Override
    public RuntimeVersionVo convert(RuntimeVersionEntity entity)
            throws ConvertException {
        return RuntimeVersionVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getVersionName())
                .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                .owner(userConvertor.convert(entity.getOwner()))
                .tag(entity.getVersionTag())
                .meta(entity.getVersionMeta())
                .image(entity.getImage())
                .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
                .build();
    }

    @Override
    public RuntimeVersionEntity revert(RuntimeVersionVo runtimeVersionVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
