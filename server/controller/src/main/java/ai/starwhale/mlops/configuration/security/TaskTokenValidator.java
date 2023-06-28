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

package ai.starwhale.mlops.configuration.security;

import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.job.step.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.job.step.task.po.TaskEntity;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TaskTokenValidator implements JwtClaimValidator {

    private static final String CLAIM_TASK_ID = "taskId";
    private static final Set<TaskStatus> TOKEN_VALID_STATUSES = Set.of(
            TaskStatus.PREPARING,
            TaskStatus.RUNNING,
            TaskStatus.READY
    );
    final JwtTokenUtil jwtTokenUtil;
    final TaskMapper taskMapper;

    public TaskTokenValidator(JwtTokenUtil jwtTokenUtil, TaskMapper taskMapper) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.taskMapper = taskMapper;
    }

    public String getTaskToken(User owner, Long taskId) {
        String jwtToken = jwtTokenUtil.generateAccessToken(owner, Map.of(CLAIM_TASK_ID, taskId));
        return String.format("Bearer %s", jwtToken);
    }

    @Override
    public void validClaims(Claims claims) throws SwValidationException {
        Object taskId = claims.get(CLAIM_TASK_ID);
        if (null == taskId) {
            return;
        }
        Long tid;
        try {
            tid = ((Number) taskId).longValue();
        } catch (ClassCastException e) {
            throw new SwValidationException(ValidSubject.USER, "task claim invalid");
        }
        TaskEntity task = taskMapper.findTaskById(tid);
        if (null == task || !TOKEN_VALID_STATUSES.contains(task.getTaskStatus())) {
            throw new SwValidationException(ValidSubject.USER, "task claim status invalid");
        }
    }
}
