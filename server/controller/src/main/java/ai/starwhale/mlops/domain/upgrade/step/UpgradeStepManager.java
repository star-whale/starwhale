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

package ai.starwhale.mlops.domain.upgrade.step;

import ai.starwhale.mlops.domain.upgrade.bo.Upgrade;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

@Component
public class UpgradeStepManager {

    private final List<UpgradeStep> steps;

    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;


    public UpgradeStepManager(List<UpgradeStep> steps, ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.steps = steps;
    }

    public void runSteps(Upgrade upgrade) {
        StepTask stepTask = new StepTask(upgrade, steps);
        ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(stepTask, new PeriodicTrigger(3000));
        stepTask.setFuture(schedule);
    }


}
