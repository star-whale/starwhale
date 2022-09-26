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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobSpecParser {

    public static final String DEFAULT_JOB_NAME = "default";
    private static final YAMLMapper yamlMapper = new YAMLMapper();

    static {
        yamlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static List<StepSpec> parseStepFromYaml(String yamlContent) {
        return parseStepFromYaml(yamlContent, DEFAULT_JOB_NAME);
    }

    public static List<StepSpec> parseStepFromYaml(String yamlContent, String jobName) {
        Map<String, List<StepSpec>> map;
        try {
            map = JobSpecParser.yamlMapper.readValue(yamlContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("parse job spec yaml error.", e);
            return List.of();
        }
        return map.getOrDefault(jobName, List.of());
    }
}
