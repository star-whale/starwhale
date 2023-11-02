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

import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.SessionDao;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import org.springframework.stereotype.Service;

@Service
public class SessionInitializer implements RollingUpdateStatusListener {
    private final DataReadManager dataReadManager;
    private final SessionDao sessionDao;

    public SessionInitializer(DataReadManager dataReadManager, SessionDao sessionDao) {
        this.dataReadManager = dataReadManager;
        this.sessionDao = sessionDao;
    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) {

    }

    /**
     * deal with unFinished session at start stage
     */
    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.READY_DOWN) {
            var unFinishedSessions = sessionDao.selectUnFinished();
            for (Session session : unFinishedSessions) {
                dataReadManager.generateDataReadLog(session.getId());
            }
        }
    }
}
