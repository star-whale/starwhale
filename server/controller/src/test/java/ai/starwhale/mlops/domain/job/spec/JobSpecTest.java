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

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobSpecTest {

    static final String YAML = "default:\n"
            + "- cls_name: ''\n"
            + "  concurrency: 1\n"
            + "  needs: []\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1\n"
            + "    limit: 1\n"
            + "  env: []\n"
            + "  name: ppl\n"
            + "  show_name: ppl\n"
            + "  replicas: 1\n"
            + "- cls_name: ''\n"
            + "  concurrency: 1\n"
            + "  needs:\n"
            + "  - ppl\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1\n"
            + "    limit: 1\n"
            + "  env: []\n"
            + "  name: cmp\n"
            + "  show_name: cmp\n"
            + "  replicas: 1\n";

    static final String YAML1 = "- cls_name: ''\n"
            + "  concurrency: 1\n"
            + "  job_name: default\n"
            + "  needs: []\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1\n"
            + "    limit: 1\n"
            + "  env: []\n"
            + "  name: ppl\n"
            + "  show_name: ppl\n"
            + "  replicas: 1\n"
            + "- cls_name: ''\n"
            + "  concurrency: 1\n"
            + "  job_name: default\n"
            + "  needs:\n"
            + "  - ppl\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1\n"
            + "    limit: 1\n"
            + "  env: []\n"
            + "  name: cmp\n"
            + "  show_name: cmp\n"
            + "  replicas: 1\n";

    static final String YAML2 = "---\n"
            + "default:\n"
            + "- name: \"ppl\"\n"
            + "  concurrency: 1\n"
            + "  replicas: 1\n"
            + "  needs: []\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  job_name: \"default\"\n"
            + "  show_name: \"ppl\"\n"
            + "- name: \"cmp\"\n"
            + "  concurrency: 1\n"
            + "  replicas: 1\n"
            + "  needs:\n"
            + "  - \"ppl\"\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  job_name: \"default\"\n"
            + "  show_name: \"cmp\"\n";

    static final String YAML3 = "---\n"
            + "- name: \"ppl\"\n"
            + "  concurrency: 1\n"
            + "  replicas: 1\n"
            + "  needs: []\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  job_name: \"default\"\n"
            + "  show_name: \"ppl\"\n"
            + "- name: \"cmp\"\n"
            + "  concurrency: 1\n"
            + "  replicas: 1\n"
            + "  needs:\n"
            + "  - \"ppl\"\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  job_name: \"default\"\n"
            + "  show_name: \"cmp\"\n";

    static final String YAML4 = "serving:\n"
            + "- concurrency: 1\n"
            + "  expose: 8080\n"
            + "  extra_kwargs:\n"
            + "    search_modules:\n"
            + "    - evaluator\n"
            + "  func_name: StandaloneModel._serve_handler\n"
            + "  module_name: starwhale.core.model.model\n"
            + "  name: serving\n"
            + "  replicas: 1\n"
            + "  service_spec:\n"
            + "    apis:\n"
            + "    - components:\n"
            + "      - component_spec_value_type: FLOAT\n"
            + "        name: temperature\n"
            + "      - component_spec_value_type: STRING\n"
            + "        name: user_input\n"
            + "      - component_spec_value_type: LIST\n"
            + "        name: history\n"
            + "      inference_type: llm_chat\n"
            + "      uri: online_eval\n"
            + "    version: 0.0.2\n"
            + "  show_name: virtual handler for model serving\n"
            + "  virtual: true\n";

    private JobSpecParser jobSpecParser;

    @BeforeEach
    public void setUp() {
        jobSpecParser = new JobSpecParser();
    }

    @Test
    public void testReadFromYaml1() throws JsonProcessingException {
        validSteps(jobSpecParser.parseAndFlattenStepFromYaml(YAML));
        validSteps(jobSpecParser.parseAndFlattenStepFromYaml(YAML1));
        validSteps(jobSpecParser.parseAndFlattenStepFromYaml(YAML2));
        validSteps(jobSpecParser.parseAndFlattenStepFromYaml(YAML3));
    }

    @Test
    public void testReadFromYamlForSvc() throws JsonProcessingException {
        var svc = jobSpecParser.parseAndFlattenStepFromYaml(YAML4);
        Assertions.assertEquals(1, svc.size());
        Assertions.assertEquals(3, svc.get(0).getServiceSpec().getApis().get(0).getComponents().size());
    }

    private void validSteps(List<StepSpec> steps) {
        Assertions.assertEquals(2, steps.size());
        StepSpec pplStep = steps.get(0);
        Assertions.assertEquals(StepSpec.builder()
                .jobName("default")
                .needs(List.of())
                .env(List.of())
                .resources(List.of(new RuntimeResource("cpu", 1f, 1f)))
                .name("ppl")
                .showName("ppl")
                .replicas(1)
                .concurrency(1)
                .build(), pplStep);
        StepSpec cmpStep = steps.get(1);
        Assertions.assertEquals(StepSpec.builder()
                .jobName("default")
                .needs(List.of("ppl"))
                .env(List.of())
                .resources(List.of(new RuntimeResource("cpu", 1f, 1f)))
                .name("cmp")
                .showName("cmp")
                .replicas(1)
                .concurrency(1)
                .build(), cmpStep);
    }

    @Test
    public void testWriteToYaml() throws JsonProcessingException {
        Map<String, List<StepSpec>> map = Map.of("default", List.of(
                StepSpec.builder()
                    .jobName("default")
                    .needs(List.of())
                    .env(List.of())
                    .resources(List.of(new RuntimeResource("cpu", 1f, 1f)))
                    .name("ppl")
                    .showName("ppl")
                    .replicas(1)
                    .concurrency(1)
                    .build(),
                StepSpec.builder()
                    .jobName("default")
                    .needs(List.of("ppl"))
                    .env(List.of())
                    .resources(List.of(new RuntimeResource("cpu", 1f, 1f)))
                    .name("cmp")
                    .showName("cmp")
                    .replicas(1)
                    .concurrency(1)
                    .build()
        ));
        Assertions.assertEquals(YAML2, new YAMLMapper().writeValueAsString(map));
        Assertions.assertEquals(YAML3, new YAMLMapper().writeValueAsString(map.get("default")));
    }

}
