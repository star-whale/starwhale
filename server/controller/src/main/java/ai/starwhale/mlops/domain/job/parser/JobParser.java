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

package ai.starwhale.mlops.domain.job.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class JobParser {
    private static final String DEFAULT_JOB_NAME = "default";
    private static final YAMLMapper yamlMapper = new YAMLMapper();

    static {
        yamlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static List<StepMetaData> parseStepFromYaml(String yamlContent) {
        return parseStepFromYaml(yamlContent, DEFAULT_JOB_NAME);
    }

    public static List<StepMetaData> parseStepFromYaml(String yamlContent, String job) {
        Map<String, List<StepMetaData>> map;
        try {
            map = JobParser.yamlMapper.readValue(yamlContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("parse job yaml error.", e);
            return List.of();
        }
        return map.getOrDefault(job, List.of());
    }

    public static void main(String[] args) throws JsonProcessingException {

        String content = "default:\n" +
            "- !!python/object:starwhale.core.job.model.Step\n" +
            "  concurrency: 1\n" +
            "  dependency:\n" +
            "  - ''\n" +
            "  job_name: default\n" +
            "  resources:\n" +
            "  - cpu=1\n" +
            "  status: ''\n" +
            "  step_name: DefaultPipeline.ppl\n" +
            "  task_num: 1\n" +
            "  tasks: []\n" +
            "- !!python/object:starwhale.core.job.model.Step\n" +
            "  concurrency: 1\n" +
            "  dependency:\n" +
            "  - DefaultPipeline.ppl\n" +
            "  job_name: default\n" +
            "  resources:\n" +
            "  - cpu=1\n" +
            "  status: ''\n" +
            "  step_name: DefaultPipeline.cmp\n" +
            "  task_num: 1\n" +
            "  tasks: []";
        Map<String, List<StepMetaData>> map = JobParser.yamlMapper.readValue(content, new TypeReference<Map<String, List<StepMetaData>>>() {
        });

        System.out.println(map.size());
    }
}
