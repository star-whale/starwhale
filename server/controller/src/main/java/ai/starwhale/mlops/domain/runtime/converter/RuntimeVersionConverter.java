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
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuntimeVersionConverter {

    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;

    private final DockerSetting dockerSetting;

    public RuntimeVersionConverter(IdConverter idConvertor,
                                   VersionAliasConverter versionAliasConvertor,
                                   DockerSetting dockerSetting) {
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.dockerSetting = dockerSetting;
    }

    public RuntimeVersionVo convert(RuntimeVersionEntity entity)
            throws ConvertException {
        return convert(entity, null, null);
    }

    public RuntimeVersionVo convert(RuntimeVersionEntity entity, RuntimeVersionEntity latest, List<String> tags)
            throws ConvertException {
        return RuntimeVersionVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getVersionName())
                .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                .latest(entity.getId() != null && latest != null && entity.getId().equals(latest.getId()))
                .tags(tags)
                .meta(entity.getVersionMeta())
                .image(entity.getImage(dockerSetting.getRegistryForPull()))
                .builtImage(entity.getBuiltImage())
                .shared(entity.getShared())
                .createdTime(entity.getCreatedTime().getTime())
                .runtimeId(idConvertor.convert(entity.getRuntimeId()))
                .build();
    }

}
