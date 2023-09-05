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

package ai.starwhale.mlops.domain.upgrade;

import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import ai.starwhale.mlops.domain.upgrade.mapper.ServerStatusMapper;
import ai.starwhale.mlops.domain.upgrade.mapper.UpgradeLogMapper;
import ai.starwhale.mlops.domain.upgrade.po.UpgradeLogEntity;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty("sw.upgrade.enabled")
public class MySqlUpgradeAccess implements UpgradeAccess {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_UPGRADING = "UPGRADING";
    private static final String MODULE_CONTROLLER = "CONTROLLER";

    private final ServerStatusMapper serverStatusMapper;
    private final UpgradeLogMapper upgradeLogMapper;

    public MySqlUpgradeAccess(ServerStatusMapper serverStatusMapper, UpgradeLogMapper upgradeLogMapper) {
        this.serverStatusMapper = serverStatusMapper;
        this.upgradeLogMapper = upgradeLogMapper;
    }


    @Override
    public boolean isUpgrading() {
        try {
            String status = serverStatusMapper.getModuleStatus(MODULE_CONTROLLER);
            return Objects.equals(STATUS_UPGRADING, status);
        } catch (Exception e) {
            throw new SwProcessException(ErrorType.DB, String.format("%s -> %s", e.getClass(), e.getMessage()));
        }

    }

    @Override
    public void setStatusToUpgrading(String progressId) {
        serverStatusMapper.updateModule(MODULE_CONTROLLER, progressId, STATUS_UPGRADING);
    }

    @Override
    public void setStatusToNormal() {
        serverStatusMapper.updateModule(MODULE_CONTROLLER, "", STATUS_NORMAL);
    }

    @Override
    public String getUpgradeProcessId() {
        return null;
    }

    @Override
    public void writeLog(UpgradeLog log) {
        upgradeLogMapper.insert(UpgradeLogEntity.builder()
                .progressUuid(log.getProgressUuid())
                .stepCurrent(log.getStepCurrent())
                .stepTotal(log.getStepTotal())
                .title(log.getTitle())
                .content(log.getContent())
                .status(log.getStatus())
                .build());
    }

    @Override
    public void updateLog(UpgradeLog log) {
        upgradeLogMapper.update(UpgradeLogEntity.builder()
                .progressUuid(log.getProgressUuid())
                .stepCurrent(log.getStepCurrent())
                .status(log.getStatus())
                .build());
    }

    @Override
    public List<UpgradeLog> readLog(String processId) {
        return upgradeLogMapper.list(processId)
                .stream()
                .map(entity -> UpgradeLog.builder()
                        .progressUuid(entity.getProgressUuid())
                        .stepCurrent(entity.getStepCurrent())
                        .stepTotal(entity.getStepTotal())
                        .title(entity.getTitle())
                        .content(entity.getContent())
                        .status(entity.getStatus())
                        .createdTime(entity.getCreatedTime().getTime())
                        .build())
                .collect(Collectors.toList());
    }
}
