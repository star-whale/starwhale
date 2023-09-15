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

package ai.starwhale.mlops.domain.model.converter;

import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.exception.ConvertException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ModelVersionVoConverter {

    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;
    private final JobSpecParser jobSpecParser;

    public ModelVersionVoConverter(IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor,
            JobSpecParser jobSpecParser) {
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.jobSpecParser = jobSpecParser;
    }

    public ModelVersionVo convert(
            ModelVersionEntity entity,
            ModelVersionEntity latest,
            List<String> tags
    ) throws ConvertException {
        try {
            return ModelVersionVo.builder()
                    .id(idConvertor.convert(entity.getId()))
                    .name(entity.getVersionName())
                    .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                    .latest(entity.getId() != null && entity.getId().equals(latest.getId()))
                    .tags(tags)
                    .builtInRuntime(entity.getBuiltInRuntime())
                    .createdTime(entity.getCreatedTime().getTime())
                    .stepSpecs(jobSpecParser.parseAndFlattenStepFromYaml(entity.getJobs()))
                    .size(entity.getStorageSize())
                    .shared(entity.getShared())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("convert ModelVersionVo error", e);
            throw new ConvertException(e.getMessage());
        }
    }

}
