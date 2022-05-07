/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import static ai.starwhale.mlops.common.util.HttpUtil.error;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    private static final String AUTH_HEADER = "Authorization";

    public JwtTokenFilter(JwtTokenUtil jwtTokenUtil, UserService userService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = httpServletRequest.getHeader(AUTH_HEADER);

        if (!checkHeader(header)) {
            header = httpServletRequest.getParameter(AUTH_HEADER);
            if(!checkHeader(header)) {
                error(httpServletResponse, HttpStatus.FORBIDDEN.value(), Code.accessDenied,
                    "Not logged in.");
                return;
            }
        }

        String token = header.split(" ")[1].trim();

        if(!jwtTokenUtil.validate(token)) {
            error(httpServletResponse, HttpStatus.FORBIDDEN.value(), Code.accessDenied, "JWT token is expired or invalid.");
            return;
        }

        Claims claims = jwtTokenUtil.parseJWT(token);

        User user = User.builder()
            .id(jwtTokenUtil.getUserId(claims))
            .name(jwtTokenUtil.getUsername(claims))
            .build();
        user = userService.loadUserByUsername(user.getName());
        JwtLoginToken jwtLoginToken = new JwtLoginToken(user, "", user.getAuthorities());
        jwtLoginToken.setDetails(new WebAuthenticationDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(jwtLoginToken);
        filterChain.doFilter(httpServletRequest, httpServletResponse);

    }

    private boolean checkHeader(String header) {
        return StringUtils.hasText(header) && header.startsWith("Bearer ");
    }
}
