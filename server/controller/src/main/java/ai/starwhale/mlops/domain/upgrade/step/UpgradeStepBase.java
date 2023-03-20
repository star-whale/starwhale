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

import ai.starwhale.mlops.domain.upgrade.UpgradeAccess;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade;
import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class UpgradeStepBase implements UpgradeStep {

    protected final UpgradeAccess upgradeAccess;

    protected UpgradeStep next;

    protected Upgrade upgrade;

    protected int totalNum;

    protected int current;

    protected boolean isStarted;

    public UpgradeStepBase(UpgradeAccess upgradeAccess) {
        this.upgradeAccess = upgradeAccess;
        this.isStarted = false;
    }


    @Override
    public void run(Upgrade upgrade, int totalNum, int current) {
        this.upgrade = upgrade;
        this.totalNum = totalNum;
        this.current = current;

        log.info(String.format("Upgrade step  %d/%d %s", current, totalNum, getTitle()));
        before();
        UpgradeLog log = UpgradeLog.builder()
                .progressUuid(upgrade.getProgressId())
                .stepCurrent(current)
                .stepTotal(totalNum)
                .title(getTitle())
                .content(getContent())
                .status("START")
                .build();
        upgradeAccess.writeLog(log);

        doStep(upgrade);

        isStarted = true;
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void complete() {
        log.info(String.format("Upgrade step  %d/%d %s complete.", current, totalNum, getTitle()));
        UpgradeLog log = UpgradeLog.builder()
                .progressUuid(upgrade.getProgressId())
                .stepCurrent(current)
                .stepTotal(totalNum)
                .title(getTitle())
                .content(getContent())
                .status("COMPLETE")
                .build();
        upgradeAccess.updateLog(log);
        after();
    }

    protected abstract void doStep(Upgrade upgrade);

    protected abstract String getTitle();

    protected abstract String getContent();


    protected void before() {

    }

    protected void after() {

    }
}
