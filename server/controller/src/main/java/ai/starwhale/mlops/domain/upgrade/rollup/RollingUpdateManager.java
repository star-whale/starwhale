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

    private final RestTemplate restTemplate;

    private final String oldInstanceAddress;
    private final String apiPrefix;

    private final UserService userService;

    private final JwtTokenUtil jwtTokenUtil;

    static final String PARAM_NAME_STATUS = "status";

    public RollingUpdateManager(
            List<RollingUpdateStatusListener> rollingUpdateStatusListeners, RestTemplateBuilder restTemplateBuilder,
            @Value("${sw.rollup.instance-uri-old}") String oldInstanceAddress,
            @Value("${sw.rollup.timeout-connect-seconds}") int timeoutConnectionSeconds,
            @Value("${sw.rollup.timeout-read-hours}") int timeoutReadHours,
            @Value("${sw.controller.api-prefix}") String apiPrefix, UserService userService,
            JwtTokenUtil jwtTokenUtil
    ) {
        this.rollingUpdateStatusListeners = rollingUpdateStatusListeners;
        this.apiPrefix = apiPrefix;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(timeoutConnectionSeconds))
                .setReadTimeout(Duration.ofHours(timeoutReadHours))
                .build();
        this.oldInstanceAddress = oldInstanceAddress;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("rolling update manager starting ...");
        User superUser = userService.loadUserById(1L);
        String superToken = jwtTokenUtil.generateAccessToken(superUser);
        ResponseEntity<ResponseMessage> objectResponseEntity;
        try {
            objectResponseEntity =
                    restTemplate.postForEntity(
                            rollupNotifyAddress(),
                            null,
                            ResponseMessage.class,
                            Map.of(
                                    PARAM_NAME_STATUS,
                                    ServerInstanceStatus.READY_UP,
                                    JwtTokenFilter.AUTH_HEADER,
                                    String.format("Bearer %s", superToken)
                            )
                    );
        } catch (Exception e) {
            log.info("seems that no old instance is up now, just goes up directly");
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
            }
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.DOWN);
            }
            return;

        }
        if (Code.internalServerError.getType().equals(objectResponseEntity.getBody().getCode())) {
            log.error("the old instance failed to do staff related to rolling upgrade ,please do upgrade manually");
            System.exit(1);
        } else {
            try {
                for (var l : rollingUpdateStatusListeners) {
                    l.onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
                }
            } catch (Throwable e) {
                log.error(
                        "the new instance failed to do staff related to rolling upgrade ,please do upgrade manually"
                        );
                restTemplate.postForEntity(
                        rollupNotifyAddress(),
                        null,
                        ResponseMessage.class,
                        Map.of(
                                PARAM_NAME_STATUS,
                                ServerInstanceStatus.DOWN,
                                JwtTokenFilter.AUTH_HEADER,
                                superToken
                        )
                );
                System.exit(1);
            }
            try{
                restTemplate.postForEntity(
                        rollupNotifyAddress(),
                        null,
                        ResponseMessage.class,
                        Map.of(
                                PARAM_NAME_STATUS,
                                ServerInstanceStatus.UP,
                                JwtTokenFilter.AUTH_HEADER,
                                String.format("Bearer %s", superToken)
                        )
                );
            }catch (Exception e){
                log.warn("there is something wrong with old server instance, but the new server still goes up");
            }
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.DOWN);
            }

        }
    }

    @NotNull
    private String rollupNotifyAddress() {
        return oldInstanceAddress + apiPrefix + RollingUpdateController.STATUS_NOTIFY_PATH+ "?status={status}&Authorization={Authorization}";
    }
}
