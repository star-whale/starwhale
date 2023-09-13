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

package ai.starwhale.mlops.domain.upgrade.rollup.aspectcut;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import ai.starwhale.mlops.exception.SwUnavailableException;
import ai.starwhale.mlops.exception.SwUnavailableException.Reason;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class WriteOperationCare implements RollingUpdateStatusListener {

    private volatile boolean forbidWrite = true;

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.BORN) {
            this.forbidWrite = true;
        } else if (status == ServerInstanceStatus.DOWN) {
            this.forbidWrite = false;
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.DOWN) {
            this.forbidWrite = false;
        }
    }

    @Before("@annotation(ai.starwhale.mlops.domain.upgrade.rollup.aspectcut.WriteOperation)")
    public void writeCare(JoinPoint joinPoint) throws Throwable {
        if (forbidWrite) {
            throw new SwUnavailableException(Reason.UPGRADING, "Server is upgrading");
        }
    }

}
