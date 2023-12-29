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


import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.domain.job.spec.step.StepSpec;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StepSpecTest {

    @Test
    public void testNoArgs() {
        StepSpec spec = new StepSpec();
        spec.setExtCmdArgs("--a 123 --b dads");
        spec.verifyStepSpecArgs();
    }

    @Test
    public void testRequiredArgs() {
        StepSpec spec = new StepSpec();
        spec.setParametersSig(List.of(ParameterSignature.builder().name("a").required(true).build()));
        spec.setExtCmdArgs("");
        Assertions.assertThrows(SwValidationException.class, spec::verifyStepSpecArgs);
    }

    @Test
    public void testNonRequiredArgs() {

        StepSpec spec = new StepSpec();
        spec.setParametersSig(List.of(ParameterSignature.builder().name("a").required(false).build()));
        spec.setExtCmdArgs("--a dads");
        spec.verifyStepSpecArgs();
    }

    @Test
    public void testExtraRequiredArgs() {
        StepSpec spec = new StepSpec();
        spec.setParametersSig(List.of(ParameterSignature.builder().name("a").required(true).build()));
        spec.setExtCmdArgs("--a 123 --b dads");
        Assertions.assertThrows(SwValidationException.class, spec::verifyStepSpecArgs);
    }

    @Test
    public void testMalformedArgs() {
        StepSpec spec = new StepSpec();
        spec.setParametersSig(List.of(ParameterSignature.builder().name("a").required(false).build()));
        spec.setExtCmdArgs("-casdaf");
        Assertions.assertThrows(SwValidationException.class, spec::verifyStepSpecArgs);
    }

    @Test
    public void testMultipleArgs() {
        StepSpec spec = new StepSpec();
        spec.setParametersSig(List.of(ParameterSignature.builder().name("a").required(false).multiple(true).build()));
        spec.setExtCmdArgs("--a=1 -a=2 a=3");
        spec.verifyStepSpecArgs();
    }

    @Test
    public void testOptionWithoutArgument() {
        StepSpec spec = new StepSpec();
        spec.setParametersSig(List.of(ParameterSignature.builder().name("a").required(true).build()));
        spec.setExtCmdArgs("--a");
        Assertions.assertThrows(SwValidationException.class, spec::verifyStepSpecArgs);
    }

    @Test
    public void testFriendlyName() {
        var stepSpec = StepSpec.builder()
                .name("name")
                .showName("the show name")
                .build();

        assertEquals("the show name", stepSpec.getFriendlyName());

        stepSpec.setShowName(null);
        assertEquals("name", stepSpec.getFriendlyName());

        stepSpec.setName("serving");
        assertEquals("serving", stepSpec.getFriendlyName());
    }

    @Test
    public void testToJson() {
        var stepSpec = StepSpec.builder()
                .name("name")
                .showName("the show name")
                .build();

        var jobSpecParser = new JobSpecParser();
        var jsonStr = jobSpecParser.stepToJsonQuietly(stepSpec);
        assertEquals("{\"name\":\"name\",\"show_name\":\"the show name\"}", jsonStr);

        var stepSpec2 = jobSpecParser.stepFromJsonQuietly(jsonStr);
        stepSpec2.setConcurrency(null);
        stepSpec2.setReplicas(null);
        assertEquals(stepSpec, stepSpec2);
    }
}
