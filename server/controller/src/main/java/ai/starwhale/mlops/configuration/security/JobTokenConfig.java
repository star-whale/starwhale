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
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class JobTokenConfig implements CommandLineRunner {

    final JwtTokenUtil jwtTokenUtil;

    final Long jobUserId;

    final UserService userService;

    @Getter
    String token;

    public JobTokenConfig(JwtTokenUtil jwtTokenUtil,
        @Value("${sw.jwt.job-user-id}") Long jobUserId,
        UserService userService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jobUserId = jobUserId;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        User user = userService.loadUserById(jobUserId);
        this.token = jwtTokenUtil.generateAccessToken(user,null);
    }
}
