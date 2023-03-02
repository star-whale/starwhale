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
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade.STATUS;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class StepTask implements Runnable {

    private final Upgrade upgrade;
    private final List<UpgradeStep> steps;

    private ScheduledFuture<?> future;

    private int current;


    public StepTask(Upgrade upgrade, List<UpgradeStep> steps) {
        this.upgrade = upgrade;
        this.steps = steps;
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    @Override
    public synchronized void run() {
        UpgradeStep step = runCurrent();
        if (step != null && step.isComplete()) {
            // complete
            step.complete();
            current++;

            runCurrent();
        }

    }

    private UpgradeStep runCurrent() {
        if (current < steps.size()) {
            UpgradeStep step = steps.get(current);
            if (!step.isStarted()) {
                step.run(upgrade, steps.size(), current + 1);
            }
            return step;
        } else {
            future.cancel(false);
            future = null;
            upgrade.setStatus(STATUS.COMPLETE);
            return null;
        }
    }
}
