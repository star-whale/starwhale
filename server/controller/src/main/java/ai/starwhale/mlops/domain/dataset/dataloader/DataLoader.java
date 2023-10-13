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

import static ai.starwhale.mlops.exception.SwRequestFrequentException.RequestType.DATASET_LOAD;

import ai.starwhale.mlops.common.KeyLock;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwRequestFrequentException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataLoader {
    private final DataReadManager dataReadManager;
    private final Integer lockWaitSeconds;

    public DataLoader(DataReadManager dataReadManager,
                      @Value("${sw.dataset.load.read.lock-wait-seconds}") int lockWaitSeconds) {
        this.dataReadManager = dataReadManager;
        this.lockWaitSeconds = lockWaitSeconds;
    }

    public DataReadLog next(DataReadRequest request) {
        var consumerId = request.getConsumerId();
        var sessionId = request.getSessionId();
        var datasetVersionId = request.getDatasetVersionId();
        Session session = dataReadManager.getSession(request);
        if (session == null) {
            // ensure serially in the same session with the same dataset version
            // this lock should wrap the transaction and use double check
            session = lockOrThrow(String.format("dl-session-generate-%s-%s", sessionId, datasetVersionId), () -> {
                var s = dataReadManager.getSession(request);
                if (s == null) {
                    s = dataReadManager.generateSession(request);
                }
                return s;
            }, "data load: session init");
        }

        dataReadManager.handleConsumerData(consumerId, request.getProcessedData(), session);

        // this lock can be replaced by select fot update in future
        Session finalSession = session;
        return lockOrThrow(String.format("dl-assignment-%s", session.getId()), () ->
                dataReadManager.assignmentData(consumerId, finalSession), "data load: assignment data"
        );
    }

    private <T> T lockOrThrow(String lockKey, Supplier<T> supplier, String errorMessage) {
        var lock = new KeyLock<>(lockKey);
        try {
            if (!lock.tryLock(lockWaitSeconds, TimeUnit.SECONDS)) {
                throw new SwRequestFrequentException(DATASET_LOAD, errorMessage);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            throw new SwProcessException(SwProcessException.ErrorType.SYSTEM, errorMessage);
        } finally {
            lock.unlock();
        }
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
