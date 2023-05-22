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

package ai.starwhale.mlops.domain.dataset.dataloader;

import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import org.springframework.stereotype.Service;

@Service
public class DataLoader {
    private final DataReadManager dataReadManager;

    public DataLoader(DataReadManager dataReadManager) {
        this.dataReadManager = dataReadManager;
    }

    public DataReadLog next(DataReadRequest request) {
        var consumerId = request.getConsumerId();
        var session = dataReadManager.getOrGenerateSession(request);

        dataReadManager.handleConsumerData(consumerId, request.isSerial(), request.getProcessedData(), session);

        return dataReadManager.assignmentData(consumerId, session);
    }
}
