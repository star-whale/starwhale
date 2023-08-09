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

import static ai.starwhale.mlops.common.util.HttpUtil.error;

import ai.starwhale.mlops.api.protocol.Code;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String msg;
        if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
            msg = "Wrong username or password";
        } else if (exception instanceof LockedException) {
            msg = "The account is locked, please contact the administrator";
        } else if (exception instanceof CredentialsExpiredException) {
            msg = "Password expired, please contact the administrator!";
        } else if (exception instanceof AccountExpiredException) {
            msg = "The account has expired, please contact the administrator";
        } else if (exception instanceof DisabledException) {
            msg = "The account is disabled, please contact the administrator.";
        } else {
            msg = exception.getCause().getMessage();
        }

        error(response, HttpStatus.UNAUTHORIZED.value(), Code.Unauthorized, msg);

    }
}
