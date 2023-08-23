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
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.api-prefix}")
public class LogController implements LogApi {

    final TaskService taskService;

    public LogController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public ResponseEntity<ResponseMessage<List<String>>> offlineLogs(Long taskId) {
        return ResponseEntity.ok(Code.success.asResponse(taskService.offLineLogFiles(taskId)));
    }

    @Override
    public ResponseEntity<String> logContent(Long taskId, String fileName) {
        return ResponseEntity.ok(taskService.logContent(taskId, fileName));
    }

}
