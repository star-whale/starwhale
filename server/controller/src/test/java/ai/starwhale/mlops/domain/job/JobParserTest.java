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
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JobParserTest {
    @Test
    public void testParseFromYamlContent() throws JsonProcessingException {
        String yamlContent = "default:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  job_name: default\n"
                + "  needs: []\n"
                + "  resources: []\n"
                + "  name: ppl\n"
                + "  task_num: 1\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  job_name: default\n"
                + "  needs:\n"
                + "  - ppl\n"
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
                + "  name: cmp\n"
                + "  task_num: 1";
        JobSpecParser jobSpecParser = new JobSpecParser();
        List<StepSpec> stepMetaDatas = jobSpecParser.parseStepFromYaml(yamlContent);
        Assertions.assertEquals(stepMetaDatas.size(), 2);
        Assertions.assertEquals(stepMetaDatas.get(0).getResources().size(), 0);
        Assertions.assertEquals(stepMetaDatas.get(1).getResources().size(), 3);
    }
}
