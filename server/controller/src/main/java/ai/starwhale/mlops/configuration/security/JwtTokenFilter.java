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
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwValidationException;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    private final ProjectService projectService;

    private final List<JwtClaimValidator> jwtClaimValidators;

    private static final String AUTH_HEADER = "Authorization";

    public JwtTokenFilter(JwtTokenUtil jwtTokenUtil, UserService userService, ProjectService projectService,
            List<JwtClaimValidator> jwtClaimValidators) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        this.projectService = projectService;
        this.jwtClaimValidators = jwtClaimValidators;
    }

    boolean allowAnonymous(HttpServletRequest request) {
        var projects = getProjects(request);
        // only for public project
        return projects.stream().allMatch(p -> p.getPrivacy() == Project.Privacy.PUBLIC);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
            @NonNull HttpServletResponse httpServletResponse,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = httpServletRequest.getHeader(AUTH_HEADER);

        if (!checkHeader(header)) {
            header = httpServletRequest.getParameter(AUTH_HEADER);
        }
        if (checkHeader(header)) {
            String token = header.split(" ")[1].trim();
            Claims claims;
            try {
                claims = jwtTokenUtil.parseJwt(token);
                jwtClaimValidators.forEach(cv -> cv.validClaims(claims));
            } catch (SwValidationException e) {
                error(httpServletResponse, HttpStatus.UNAUTHORIZED.value(), Code.accessDenied,
                        "JWT token is expired or invalid.");
                return;
            }

            User user = userService.loadUserByUsername(jwtTokenUtil.getUsername(claims));
            try {
                user.defaultChecks();
            } catch (AccountStatusException e) {
                error(httpServletResponse, HttpStatus.FORBIDDEN.value(), Code.accessDenied, e.getMessage());
                return;
            }
            // Get the roles of System(whether it has owner role)
            List<Role> sysRoles = userService.getProjectRolesOfUser(user, Project.system());
            Set<Role> roles = sysRoles.stream().filter(
                    role -> role.getAuthority().equals(Role.CODE_OWNER)).collect(Collectors.toSet());
            // Get project roles
            try {
                Set<Project> projects = getProjects(httpServletRequest);
                Set<Role> rolesOfUser = userService.getProjectsRolesOfUser(user, projects);
                roles.addAll(rolesOfUser);
            } catch (StarwhaleException e) {
                logger.error(e.getMessage());
            }
            user.setRoles(roles);
            // Build jwt token with user
            JwtLoginToken jwtLoginToken = new JwtLoginToken(user, "", user.getRoles());
            jwtLoginToken.setDetails(new WebAuthenticationDetails(httpServletRequest));
            SecurityContextHolder.getContext().setAuthentication(jwtLoginToken);
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);

    }

    @NotNull
    private Set<Project> getProjects(HttpServletRequest httpServletRequest) {
        @SuppressWarnings("unchecked")
        Set<Project> projects = ((Set<String>) httpServletRequest
                .getAttribute(ProjectDetectionFilter.ATTRIBUTE_PROJECT))
                .stream()
                .map(projectService::findProject)
                .collect(Collectors.toSet());
        return projects;
    }

    private boolean checkHeader(String header) {
        return StringUtils.hasText(header) && header.startsWith("Bearer ");
    }
}
