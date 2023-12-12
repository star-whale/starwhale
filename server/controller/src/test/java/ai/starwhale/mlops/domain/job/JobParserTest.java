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

import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.step.Env;
import ai.starwhale.mlops.domain.job.spec.step.StepSpec;
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
                + "  fine_tune:\n"
                + "    require_train_datasets: true\n"
                + "    require_validation_datasets: false\n"
                + "  env:\n"
                + "    - name: EVAL_MODE\n"
                + "      value: 'test'\n"
                + "    - name: EVAL_DATASET\n"
                + "      value: 'imagenet'\n"
                + "    - name: EVAL_MODEL\n"
                + "      value: 'resnet50'";
        JobSpecParser jobSpecParser = new JobSpecParser();
        List<StepSpec> stepMetaDatas = jobSpecParser.parseAndFlattenStepFromYaml(yamlContent);
        Assertions.assertEquals(stepMetaDatas.size(), 3);
        Assertions.assertEquals(stepMetaDatas.get(0).getResources().size(), 0);
        Assertions.assertNull(stepMetaDatas.get(0).getEnv());
        Assertions.assertEquals(stepMetaDatas.get(1).getResources().size(), 3);
        Assertions.assertNull(stepMetaDatas.get(1).getEnv());
        Assertions.assertEquals(stepMetaDatas.get(2).getEnv().size(), 3);
        Assertions.assertEquals(stepMetaDatas.get(2).getEnv(), List.of(
                new Env("EVAL_MODE", "test"),
                new Env("EVAL_DATASET", "imagenet"),
                new Env("EVAL_MODEL", "resnet50")
            ));
        Assertions.assertTrue(stepMetaDatas.get(2).getFinetune().getRequireTrainDatasets());
        Assertions.assertFalse(stepMetaDatas.get(2).getFinetune().getRequireValidationDatasets());
    }
}
