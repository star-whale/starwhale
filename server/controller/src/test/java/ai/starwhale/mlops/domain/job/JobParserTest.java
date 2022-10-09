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

import ai.starwhale.mlops.domain.job.parser.JobParser;
import ai.starwhale.mlops.domain.job.parser.StepMetaData;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JobParserTest {
    @Test
    public void testParseFromYamlContent() {
        String yamlContent = "default:\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  job_name: default\n"
                + "  needs: []\n"
                + "  resources: {}\n"
                + "  step_name: ppl\n"
                + "  task_num: 1\n"
                + "- cls_name: ''\n"
                + "  concurrency: 1\n"
                + "  job_name: default\n"
                + "  needs:\n"
                + "  - ppl\n"
                + "  resources:\n"
                + "    cpu: 0.1\n"
                + "    gpu: 1\n"
                + "    memory: 100\n"
                + "  step_name: cmp\n"
                + "  task_num: 1";
        List<StepMetaData> stepMetaDatas = JobParser.parseStepFromYaml(yamlContent);
        Assertions.assertEquals(stepMetaDatas.size(), 2);
    }
}
