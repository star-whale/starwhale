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

package ai.starwhale.mlops.domain.model;

import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ModelVersionConvertor implements Convertor<ModelVersionEntity, ModelVersionVo> {

    private final IdConvertor idConvertor;
    private final UserConvertor userConvertor;
    private final VersionAliasConvertor versionAliasConvertor;
    private final JobSpecParser jobSpecParser;

    public ModelVersionConvertor(IdConvertor idConvertor, UserConvertor userConvertor,
            VersionAliasConvertor versionAliasConvertor, JobSpecParser jobSpecParser) {
        this.idConvertor = idConvertor;
        this.userConvertor = userConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.jobSpecParser = jobSpecParser;
    }

    @Override
    public ModelVersionVo convert(ModelVersionEntity entity)
            throws ConvertException {
        try {
            return ModelVersionVo.builder()
                    .id(idConvertor.convert(entity.getId()))
                    .name(entity.getVersionName())
                    .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                    .owner(userConvertor.convert(entity.getOwner()))
                    .tag(entity.getVersionTag())
                    .meta(entity.getVersionMeta())
                    .manifest(entity.getManifest())
                    .createdTime(entity.getCreatedTime().getTime())
                    .stepSpecs(jobSpecParser.parseStepFromYaml(entity.getEvalJobs()))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("convert ModelVersionVo error", e);
            throw new ConvertException(e.getMessage());
        }
    }

    @Override
    public ModelVersionEntity revert(ModelVersionVo vo)
            throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
