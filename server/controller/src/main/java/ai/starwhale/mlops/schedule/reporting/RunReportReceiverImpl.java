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

package ai.starwhale.mlops.schedule.reporting;

import ai.starwhale.mlops.domain.run.RunDao;
import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import ai.starwhale.mlops.schedule.reporting.listener.RunUpdateListener;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class RunReportReceiverImpl implements RunReportReceiver {

    final RunMapper runMapper;

    final RunDao runDao;

    final List<RunUpdateListener> runUpdateListeners;

    public RunReportReceiverImpl(
            RunMapper runMapper,
            RunDao runDao,
            List<RunUpdateListener> runUpdateListeners
    ) {
        this.runMapper = runMapper;
        this.runDao = runDao;
        this.runUpdateListeners = runUpdateListeners
                .stream()
                .sorted(Comparator.comparingInt(RunUpdateListener::getOrder))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void receive(ReportedRun reportedRun) {
        RunEntity runEntity = runMapper.getForUpdate(reportedRun.getId());
        if (null == runEntity) {
            log.warn("illegal run found {}", reportedRun.getId());
            return;
        }
        // compare runEntity with reportedRun and update it to database
        boolean statusChanged = reportedRun.getStatus() != runEntity.getStatus();
        boolean fieldsChanged = false;
        if (statusChanged) {
            fieldsChanged = true;
            runEntity.setStatus(reportedRun.getStatus());
        }
        if (null != reportedRun.getIp() && !reportedRun.getIp().equals(runEntity.getIp())) {
            fieldsChanged = true;
            runEntity.setIp(reportedRun.getIp());
        }
        if (null != reportedRun.getFailedReason()
                && !reportedRun.getFailedReason().equals(runEntity.getFailedReason())) {
            fieldsChanged = true;
            runEntity.setFailedReason(reportedRun.getFailedReason());
        }
        if (null != reportedRun.getStopTimeMillis()
                && (null == runEntity.getFinishTime()
                    || !reportedRun.getStopTimeMillis().equals(runEntity.getFinishTime().getTime()))
        ) {
            fieldsChanged = true;
            runEntity.setFinishTime(new Date(reportedRun.getStopTimeMillis()));
        }
        if (null != reportedRun.getStartTimeMillis() && (null == runEntity.getStartTime()
                || reportedRun.getStartTimeMillis().equals(runEntity.getStartTime().getTime()))
        ) {
            fieldsChanged = true;
            runEntity.setStartTime(new Date(reportedRun.getStartTimeMillis()));
        }
        if (fieldsChanged) {
            runMapper.update(runEntity);
        }
        if (!statusChanged) {
            log.info("duplicate run status reported {} {}", reportedRun.getId(), reportedRun.getStatus());
            return;
        }
        Run run = runDao.convertEntityToBo(runEntity);
        runUpdateListeners.forEach(runUpdateListener -> {
            runUpdateListener.onRunUpdate(run);
        });

    }

}
