/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
        if (exception instanceof BadCredentialsException ||
            exception instanceof UsernameNotFoundException) {
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

        error(response, HttpStatus.FORBIDDEN.value(), Code.accessDenied, msg);

    }
}
