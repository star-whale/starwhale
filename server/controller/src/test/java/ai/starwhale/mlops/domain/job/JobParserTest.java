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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protobuf.Model.Env;
import ai.starwhale.mlops.api.protobuf.Model.RuntimeResource;
import ai.starwhale.mlops.api.protobuf.Model.StepSpec;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JobParserTest {
    @Test
    public void testParseFromYamlContent() throws JsonProcessingException {
        String yamlContent = "mnist.evaluator:MNISTInference.cmp:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs: []\n"
                + "  resources: []\n"
                + "  name: mnist.evaluator:MNISTInference.ppl\n"
                + "  replicas: 1\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs:\n"
                + "  - mnist.evaluator:MNISTInference.ppl\n"
                + "  resources:\n"
                + "  - type: cpu \n"
                + "    request: 0.1\n"
                + "    limit: 0.1\n"
                + "  - type: nvidia.com/gpu \n"
                + "    request: 1\n"
                + "    limit: 1\n"
                + "  - type: memory \n"
                + "    request: 1\n"
                + "    limit: 1\n"
                + "  name: mnist.evaluator:MNISTInference.cmp\n"
                + "  replicas: 1\n"
                + "mnist.evaluator:MNISTInference.ppl:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  needs: []\n"
                + "  resources: []\n"
                + "  name: mnist.evaluator:MNISTInference.ppl\n"
                + "  replicas: 1\n"
                + "  env:\n"
                + "    - name: EVAL_MODE\n"
                + "      value: 'test'\n"
                + "    - name: EVAL_DATASET\n"
                + "      value: 'imagenet'\n"
                + "    - name: EVAL_MODEL\n"
                + "      value: 'resnet50'";
        JobSpecParser jobSpecParser = new JobSpecParser();
        List<StepSpec> stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(yamlContent);
        Assertions.assertEquals(stepSpecs.size(), 3);

        for (StepSpec stepSpec : stepSpecs) {
            Assertions.assertEquals(stepSpec.getReplicas(), 1);
            Assertions.assertEquals(stepSpec.getConcurrency(), 1);
            if (stepSpec.getJobName().equals("mnist.evaluator:MNISTInference.ppl")) {
                // exactly match the spec
                var expected = StepSpec.newBuilder()
                        .setConcurrency(1)
                        .addAllNeeds(List.of())
                        .addAllResources(List.of())
                        .setName("mnist.evaluator:MNISTInference.ppl")
                        .setJobName("mnist.evaluator:MNISTInference.ppl")
                        .setReplicas(1)
                        .addAllEnv(List.of(
                                Env.newBuilder().setName("EVAL_MODE").setValue("test").build(),
                                Env.newBuilder().setName("EVAL_DATASET").setValue("imagenet").build(),
                                Env.newBuilder().setName("EVAL_MODEL").setValue("resnet50").build()
                        )).build();
                Assertions.assertEquals(expected, stepSpec);
            } else {
                if (stepSpec.getName().equals("mnist.evaluator:MNISTInference.cmp")) {
                    var expected = StepSpec.newBuilder()
                            .setConcurrency(1)
                            .addAllNeeds(List.of("mnist.evaluator:MNISTInference.ppl"))
                            .addAllResources(List.of(
                                    RuntimeResource.newBuilder().setType("cpu").setRequest(0.1f).setLimit(0.1f).build(),
                                    RuntimeResource.newBuilder()
                                            .setType("nvidia.com/gpu")
                                            .setRequest(1)
                                            .setLimit(1)
                                            .build(),
                                    RuntimeResource.newBuilder().setType("memory").setRequest(1).setLimit(1).build()
                            ))
                            .setName("mnist.evaluator:MNISTInference.cmp")
                            .setJobName("mnist.evaluator:MNISTInference.cmp")
                            .setReplicas(1)
                            .build();
                    Assertions.assertEquals(expected, stepSpec);
                } else {
                    var expected = StepSpec.newBuilder()
                            .setConcurrency(1)
                            .addAllNeeds(List.of())
                            .addAllResources(List.of())
                            .setName("mnist.evaluator:MNISTInference.ppl")
                            .setJobName("mnist.evaluator:MNISTInference.cmp")
                            .setReplicas(1)
                            .build();
                    Assertions.assertEquals(expected, stepSpec);
                }
            }
        }
    }
}
