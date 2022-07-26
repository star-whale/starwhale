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
import ai.starwhale.mlops.common.util.HttpUtil;
import ai.starwhale.mlops.common.util.HttpUtil.Resources;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountStatusException;
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

        User user = userService.loadUserByUsername(jwtTokenUtil.getUsername(claims));
        try {
            user.defaultChecks();
        } catch (AccountStatusException e) {
            error(httpServletResponse, HttpStatus.FORBIDDEN.value(), Code.accessDenied, e.getMessage());
            return;
        }
        // Get the roles of System
        List<Role> sysRoles = userService.getProjectRolesOfUser(user, "0");
        Set<Role> roles = sysRoles.stream().filter(
            role -> role.getAuthority().equals("OWNER")).collect(Collectors.toSet());
        // Get project roles
        String projectUrl = httpServletRequest.getParameter("project");
        if(projectUrl == null) {
            projectUrl = httpServletRequest.getParameter("projectUrl");
        }
        if(projectUrl == null) {
            projectUrl = HttpUtil.getResourceUrlFromPath(httpServletRequest.getRequestURI(), Resources.PROJECT);
        }
        try {
            List<Role> rolesOfUser = userService.getProjectRolesOfUser(user,
                projectUrl);
            roles.addAll(rolesOfUser);
        } catch (StarWhaleApiException e) {
            logger.error(e.getMessage());
        }
        user.setRoles(roles);
        // Build jwt token with user
        JwtLoginToken jwtLoginToken = new JwtLoginToken(user, "", user.getAuthorities());
        jwtLoginToken.setDetails(new WebAuthenticationDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(jwtLoginToken);
        filterChain.doFilter(httpServletRequest, httpServletResponse);

    }

    private boolean checkHeader(String header) {
        return StringUtils.hasText(header) && header.startsWith("Bearer ");
    }
}
