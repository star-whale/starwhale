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
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class SpringSchedulerCare implements RollingUpdateStatusListener {

    private volatile boolean forbidSchedule = true;

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.UP) {
            this.forbidSchedule = true;
        } else if (status == ServerInstanceStatus.DOWN) {
            this.forbidSchedule = false;
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.DOWN) {
            this.forbidSchedule = false;
        }
    }


    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object scheduleCare(ProceedingJoinPoint pjp) throws Throwable {
        if (forbidSchedule) {
            log.info("server is upgrading, spring schedulers are baned now");
            return null;
        }
        return pjp.proceed();
    }

}
