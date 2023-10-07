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

package ai.starwhale.mlops.schedule.reporting.listener.impl;

import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.reporting.listener.RunUpdateListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RunUpdateListenerForDatasetConsumption implements RunUpdateListener {

    final DataLoader dataLoader;

    public RunUpdateListenerForDatasetConsumption(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @Override
    public void onRunUpdate(Run run) {
        if (run.getStatus() == RunStatus.FAILED) {
            dataLoader.resetUnProcessed(String.valueOf(run.getTaskId()));
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
