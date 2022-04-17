/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

/**
 * User login authentication interceptor
 */
@Slf4j
public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ThreadLocal<Map<String, String>> threadLocal = new ThreadLocal<>();

    public JwtLoginFilter() {
        this.setPostOnly(false);
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
        HttpServletResponse response) throws AuthenticationException {
        if (!"POST".equals(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else {
            String userName = this.obtainUsername(request);
            String password = this.obtainPassword(request);

            threadLocal.remove();

            //Create unauthenticated credentials
            JwtLoginToken jwtLoginToken = new JwtLoginToken(userName, password);

            //Set the authentication details (ip, sessionId) to the credential
            jwtLoginToken.setDetails(new WebAuthenticationDetails(request));

            //Generate an authenticated credential. The principal in the credential is userDetails
            return this.getAuthenticationManager().authenticate(jwtLoginToken);
        }
    }

    @Override
    protected String obtainUsername(HttpServletRequest request) {
        return getParameter(request, "userName");
    }

    @Override
    protected String obtainPassword(HttpServletRequest request) {
        return getParameter(request, "userPwd");
    }

    private String getParameter(HttpServletRequest request, String param) {
        return this.getBodyParams(request).get(param);
    }


    private Map<String, String> getBodyParams(HttpServletRequest request) {
        Map<String, String> bodyParams = threadLocal.get();
        if (bodyParams == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try (InputStream is = request.getInputStream()) {
                bodyParams = objectMapper.readValue(is, Map.class);
            } catch (IOException e) {
                log.error("get body params", e);
            }
            threadLocal.set(Optional.ofNullable(bodyParams).orElse(new HashMap<>()));
        }

        return bodyParams;
    }

}
