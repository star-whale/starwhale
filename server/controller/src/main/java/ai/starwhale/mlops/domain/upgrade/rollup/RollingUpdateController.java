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
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("${sw.controller.api-prefix}")
public class RollingUpdateController {

    public static final String STATUS_NOTIFY_PATH = "/system/upgrade/instance/status";

    private final List<RollingUpdateStatusListener> rollingUpdateStatusListeners;


    public RollingUpdateController(List<RollingUpdateStatusListener> rollingUpdateStatusListeners) {
        this.rollingUpdateStatusListeners = rollingUpdateStatusListeners;
    }

    @Operation(summary = "instance status notify")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = STATUS_NOTIFY_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<ResponseMessage<String>> newInstanceStatus(
            ServerInstanceStatus status,
            InstanceType instanceType
    ) {
        try {
            if (instanceType == InstanceType.NEW) {
                for (var l : rollingUpdateStatusListeners) {
                    l.onNewInstanceStatus(status);
                }
            } else if (instanceType == InstanceType.OLD) {
                for (var l : rollingUpdateStatusListeners) {
                    l.onOldInstanceStatus(status);
                }
            } else {
                throw new IllegalArgumentException("unknown instance type");
            }
            return ResponseEntity.ok(Code.success.asResponse("old instance is ready to go down"));
        } catch (Exception e) {
            log.error("the old instance failed to do staff related to rolling upgrade ,please do upgrade manually");
            return ResponseEntity.ok(Code.internalServerError.asResponse("old instance is not ready to go down"));
        }

    }

    public enum InstanceType {
        NEW, OLD
    }
}
