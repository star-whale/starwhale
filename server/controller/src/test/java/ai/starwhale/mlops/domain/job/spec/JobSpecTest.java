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

import ai.starwhale.mlops.api.protobuf.Model.RuntimeResource;
import ai.starwhale.mlops.api.protobuf.Model.StepSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
            + "  expose: null\n"
            + "  virtual: null\n"
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
            + "  replicas: 1\n"
            + "  expose: null\n"
            + "  virtual: null\n";

    static final String YAML2 = "---\n"
            + "default:\n"
            + "- concurrency: 1\n"
            + "  needs: []\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  replicas: 1\n"
            + "  job_name: \"default\"\n"
            + "  name: \"ppl\"\n"
            + "  show_name: \"ppl\"\n"
            + "- concurrency: 1\n"
            + "  needs:\n"
            + "  - \"ppl\"\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  replicas: 1\n"
            + "  job_name: \"default\"\n"
            + "  name: \"cmp\"\n"
            + "  show_name: \"cmp\"\n";

    static final String YAML3 = "---\n"
            + "- concurrency: 1\n"
            + "  needs: []\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  replicas: 1\n"
            + "  job_name: \"default\"\n"
            + "  name: \"ppl\"\n"
            + "  show_name: \"ppl\"\n"
            + "- concurrency: 1\n"
            + "  needs:\n"
            + "  - \"ppl\"\n"
            + "  resources:\n"
            + "  - type: \"cpu\"\n"
            + "    request: 1.0\n"
            + "    limit: 1.0\n"
            + "  env: []\n"
            + "  replicas: 1\n"
            + "  job_name: \"default\"\n"
            + "  name: \"cmp\"\n"
            + "  show_name: \"cmp\"\n";

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

    private void validSteps(List<StepSpec> steps) {
        Assertions.assertEquals(2, steps.size());
        StepSpec pplStep = steps.get(0);
        Assertions.assertEquals(StepSpec.newBuilder()
                .setJobName("default")
                .addAllNeeds(List.of())
                .addAllEnv(List.of())
                .addAllResources(List.of(RuntimeResource.newBuilder()
                        .setType("cpu")
                        .setRequest(1f)
                        .setLimit(1f)
                        .build()))
                .setName("ppl")
                .setShowName("ppl")
                .setReplicas(1)
                .setConcurrency(1)
                .build(), pplStep);
        StepSpec cmpStep = steps.get(1);
        Assertions.assertEquals(StepSpec.newBuilder()
                .setJobName("default")
                .addAllNeeds(List.of("ppl"))
                .addAllEnv(List.of())
                .addAllResources(List.of(RuntimeResource.newBuilder()
                        .setType("cpu")
                        .setRequest(1f)
                        .setLimit(1f)
                        .build()))
                .setName("cmp")
                .setShowName("cmp")
                .setReplicas(1)
                .setConcurrency(1)
                .build(), cmpStep);
    }

    @Test
    @Disabled("TODO: support proto to yaml")
    public void testWriteToYaml() throws JsonProcessingException {
        Map<String, List<StepSpec>> map = Map.of("default", List.of(
                StepSpec.newBuilder()
                    .setJobName("default")
                    .addAllNeeds(List.of())
                    .addAllEnv(List.of())
                    .addAllResources(List.of(RuntimeResource.newBuilder()
                            .setType("cpu")
                            .setRequest(1f)
                            .setLimit(1f)
                            .build()))
                    .setName("ppl")
                    .setShowName("ppl")
                    .setReplicas(1)
                    .setConcurrency(1)
                    .build(),
                StepSpec.newBuilder()
                    .setJobName("default")
                    .addAllNeeds(List.of("ppl"))
                    .addAllEnv(List.of())
                    .addAllResources(List.of(RuntimeResource.newBuilder()
                            .setType("cpu")
                            .setRequest(1f)
                            .setLimit(1f)
                            .build()))
                    .setName("cmp")
                    .setShowName("cmp")
                    .setReplicas(1)
                    .setConcurrency(1)
                    .build()
        ));
        Assertions.assertEquals(YAML2, new YAMLMapper().writeValueAsString(map));
        Assertions.assertEquals(YAML3, new YAMLMapper().writeValueAsString(map.get("default")));
    }

}
