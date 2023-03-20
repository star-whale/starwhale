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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Order(1)
public class BackupDatabase extends UpgradeStepBase {

    public BackupDatabase(UpgradeAccess upgradeAccess) {
        super(upgradeAccess);
    }

    @Override
    protected void doStep(Upgrade upgrade) {
        //TODO backup mysql data
        log.info("Back up database" + upgrade.toString());
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    protected String getTitle() {
        return "Back up database";
    }

    @Override
    protected String getContent() {
        return "";
    }
}
