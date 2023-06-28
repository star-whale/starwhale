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

package ai.starwhale.mlops.api.config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.configuration.security.JwtProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.step.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.job.step.task.po.TaskEntity;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class TaskTokenValidatorTest {

    TaskTokenValidator taskTokenValidator;
    TaskMapper taskMapper = mock(TaskMapper.class);

    JwtTokenUtil jwtTokenUtil;

    User user = User.builder().id(1L).name("n").build();

    @BeforeEach
    public void setup() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("sw_secret");
        jwtTokenUtil = new JwtTokenUtil(jwtProperties);
        taskTokenValidator = new TaskTokenValidator(jwtTokenUtil, taskMapper);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"RUNNING", "READY"})
    public void testWithRunningTask(TaskStatus taskStatus) {
        long taskId = 1L;
        String token = taskTokenValidator.getTaskToken(user, taskId).split(" ")[1];
        when(taskMapper.findTaskById(eq(taskId))).thenReturn(TaskEntity.builder().taskStatus(taskStatus).build());
        taskTokenValidator.validClaims(jwtTokenUtil.parseJwt(token));
    }

    @Test
    public void testWithNullTask() {
        String token = taskTokenValidator.getTaskToken(user, 1L).split(" ")[1];

        //task is null
        when(taskMapper.findTaskById(eq(1L))).thenReturn(null);
        Assertions.assertThrows(SwValidationException.class,
                () -> taskTokenValidator.validClaims(jwtTokenUtil.parseJwt(token)));

    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"PAUSED", "FAIL", "SUCCESS", "CREATED", "CANCELED"})
    public void testWithNotRunningTask(TaskStatus taskStatus) {
        String token = taskTokenValidator.getTaskToken(user, 1L).split(" ")[1];

        //task status is not running
        when(taskMapper.findTaskById(eq(1L))).thenReturn(TaskEntity.builder().taskStatus(taskStatus).build());
        Assertions.assertThrows(SwValidationException.class,
                () -> taskTokenValidator.validClaims(jwtTokenUtil.parseJwt(token)));
    }

    @Test
    public void testWithOutTask() {
        String token = jwtTokenUtil.generateAccessToken(user);
        taskTokenValidator.validClaims(jwtTokenUtil.parseJwt(token));
    }
}
