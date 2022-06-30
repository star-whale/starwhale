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

package ai.starwhale.mlops.domain.job.step.trigger;

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StepTriggerContext {

    final List<StepTrigger> stepTriggers;

    public StepTriggerContext(
        List<StepTrigger> stepTriggers) {
        this.stepTriggers = stepTriggers;
    }
    public Step triggerNextStep(Step step){
        Step nextStep = step.getNextStep();
        if(null == nextStep){
            return null;
        }
        for(StepTrigger stepTrigger:stepTriggers){
            if(stepTrigger.applyTo(step.getJob().getType(),step.getName())){
                stepTrigger.triggerNextStep(step);
                step.getJob().setCurrentStep(nextStep);
                return nextStep;
            }
        }
        throw new SWProcessException(ErrorType.SYSTEM).tip("no trigger suit for " + step.getJob().getType().name() + ": "+step.getName());
    }
}
