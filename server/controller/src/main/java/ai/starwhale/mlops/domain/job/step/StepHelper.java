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

package ai.starwhale.mlops.domain.job.step;

import ai.starwhale.mlops.domain.job.step.bo.Step;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class StepHelper {

    public Step firsStep(List<Step> linkedSteps) {
        List<Step> followingSteps = linkedSteps.stream()
                .map(Step::getNextStep)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Optional<Step> headStepOp = linkedSteps.stream().filter(step -> !followingSteps.contains(step)).findAny();
        return headStepOp.get();
    }
}
