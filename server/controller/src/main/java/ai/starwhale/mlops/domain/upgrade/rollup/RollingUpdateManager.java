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

package ai.starwhale.mlops.domain.upgrade.rollup;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.configuration.security.JwtTokenFilter;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Order(2)
public class RollingUpdateManager implements CommandLineRunner {

    private final List<RollingUpdateStatusListener> rollingUpdateStatusListeners;

    private final Boolean rollUp;

    static final String PARAM_NAME_STATUS = "status";

    public RollingUpdateManager(
            List<RollingUpdateStatusListener> rollingUpdateStatusListeners,
            @Value("${sw.rollup}") Boolean rollUp
    ) {
        this.rollingUpdateStatusListeners = rollingUpdateStatusListeners;
        this.rollUp = rollUp;
    }

    @Override
    public void run(String... args) throws Exception {

        if(!rollUp){
            log.info("start up in normal start mode ...");
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
            }
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.DOWN);
            }
        }else {
            log.info("start up in rolling update mode, waiting for old controller instance status notify ...");
        }
    }

}
