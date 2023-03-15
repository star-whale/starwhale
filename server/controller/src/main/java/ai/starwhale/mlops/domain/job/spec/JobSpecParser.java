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

package ai.starwhale.mlops.domain.job.spec;

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class JobSpecParser {

    public static final String DEFAULT_JOB_NAME = "default";

    public List<StepSpec> parseStepFromYaml(String yamlContent) throws JsonProcessingException {
        try {
            return Constants.yamlMapper.readValue(yamlContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.info("yaml in map format, {}", yamlContent);
            return parseStepFromYaml(yamlContent, DEFAULT_JOB_NAME);
        }
    }

    public List<StepSpec> parseStepFromYaml(String yamlContent, String jobName) throws JsonProcessingException {
        Map<String, List<StepSpec>> map = Constants.yamlMapper.readValue(yamlContent, new TypeReference<>() {
        });
        List<StepSpec> specList = map.get(jobName);
        if (CollectionUtils.isEmpty(specList)) {
            log.error("step specification is empty for {}", yamlContent);
            throw new SwValidationException(ValidSubject.MODEL);
        }
        return specList;
    }
}
