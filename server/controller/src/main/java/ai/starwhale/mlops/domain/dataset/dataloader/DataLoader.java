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

import ai.starwhale.mlops.common.KeyLock;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import org.springframework.stereotype.Service;

@Service
public class DataLoader {
    private final DataReadManager dataReadManager;

    public DataLoader(DataReadManager dataReadManager) {
        this.dataReadManager = dataReadManager;
    }

    public DataReadLog next(DataReadRequest request) {
        var consumerId = request.getConsumerId();
        var sessionId = request.getSessionId();
        var datasetVersionId = request.getDatasetVersionId();
        Session session = dataReadManager.getSession(request);
        if (session == null) {
            // ensure serially in the same session with the same dataset version
            // this lock should wrap the transaction and use double check
            var sessionLock = new KeyLock<>(String.format("dl-session-generate-%s-%s", sessionId, datasetVersionId));
            try {
                sessionLock.lock();
                session = dataReadManager.getSession(request);
                if (session == null) {
                    session = dataReadManager.generateSession(request);
                }
            } finally {
                sessionLock.unlock();
            }
        }

        dataReadManager.handleConsumerData(consumerId, request.getProcessedData(), session);

        return dataReadManager.assignmentData(consumerId, session);
    }

    /**
     * Expect to continue processing data(which were unprocessed in current time) on next startup
     *
     * @param consumerId consumer id
     */
    public void resetUnProcessed(String consumerId) {
        dataReadManager.resetUnProcessedData(consumerId);
    }
}
