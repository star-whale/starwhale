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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.domain.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "Log")
@RequestMapping("${sw.controller.api-prefix}")
public class LogController {

    final TaskService taskService;

    public LogController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "list the log files of a task")
    @GetMapping(value = "/log/offline/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseMessage<List<String>>> offlineLogs(
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(Code.success.asResponse(taskService.offLineLogFiles(taskId)));
    }

    @Operation(summary = "Get the list of device types")
    @GetMapping(value = "/log/offline/{taskId}/{fileName}", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> logContent(
            @PathVariable Long taskId,
            @PathVariable String fileName
    ) {
        return ResponseEntity.ok(taskService.logContent(taskId, fileName));
    }

}
