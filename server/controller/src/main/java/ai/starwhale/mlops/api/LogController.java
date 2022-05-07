/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
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
@RequestMapping("${sw.controller.apiPrefix}")
public class LogController implements LogApi{

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
        return ResponseEntity.ok(taskService.logContent(taskId,fileName));
    }
}
