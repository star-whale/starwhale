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

import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * User role verification specific implementation
 */
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    public JwtAuthenticationProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        User user = userService.loadUserByUsername(authentication.getName());
        user.defaultChecks();

        JwtLoginToken jwtLoginToken = (JwtLoginToken) authentication;
        // Username and password verification
        user.additionalAuthenticationChecks(jwtLoginToken);
        // Create authenticated authentication
        JwtLoginToken authenticatedToken = new JwtLoginToken(user, jwtLoginToken.getCredentials(),
                user.getRoles());
        authenticatedToken.setDetails(jwtLoginToken.getDetails());
        return authenticatedToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtLoginToken.class
                .isAssignableFrom(authentication));
    }

}
