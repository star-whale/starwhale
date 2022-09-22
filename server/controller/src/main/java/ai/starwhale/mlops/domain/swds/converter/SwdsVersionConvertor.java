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

package ai.starwhale.mlops.domain.swds.converter;

import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class SwdsVersionConvertor implements Convertor<SwDatasetVersionEntity, DatasetVersionVo> {

    private final IdConvertor idConvertor;
    private final UserConvertor userConvertor;
    private final VersionAliasConvertor versionAliasConvertor;

    public SwdsVersionConvertor(IdConvertor idConvertor, UserConvertor userConvertor,
            VersionAliasConvertor versionAliasConvertor) {
        this.idConvertor = idConvertor;
        this.userConvertor = userConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    @Override
    public DatasetVersionVo convert(SwDatasetVersionEntity entity)
            throws ConvertException {
        if (entity == null) {
            return null;
        }
        return DatasetVersionVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                .name(entity.getVersionName())
                .owner(userConvertor.convert(entity.getOwner()))
                .tag(entity.getVersionTag())
                .meta(entity.getVersionMeta())
                .createdTime(entity.getCreatedTime().getTime())
                .build();
    }

    @Override
    public SwDatasetVersionEntity revert(DatasetVersionVo vo)
            throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
