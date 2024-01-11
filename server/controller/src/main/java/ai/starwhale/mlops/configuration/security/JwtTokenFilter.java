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
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwValidationException;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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
    private static final List<Pattern> WHITE_LIST_FOR_DELETED_PROJECTS = List.of(
            Pattern.compile("/api/v1/project/[^/]+/recover")
    );

    public JwtTokenFilter(
            JwtTokenUtil jwtTokenUtil,
            UserService userService,
            ProjectService projectService,
            List<JwtClaimValidator> jwtClaimValidators
    ) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        this.projectService = projectService;
        this.jwtClaimValidators = jwtClaimValidators;
    }

    boolean allowAnonymous(Set<Project> projects) {
        // only for public project
        return projects.stream().allMatch(p -> p.getPrivacy() == Project.Privacy.PUBLIC);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest httpServletRequest,
            @NonNull HttpServletResponse httpServletResponse,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = httpServletRequest.getHeader(AUTH_HEADER);

        if (isInvalidAuthHeader(header)) {
            header = httpServletRequest.getParameter(AUTH_HEADER);
        }

        Set<Project> projects;
        try {
            projects = getProjects(httpServletRequest);
        } catch (SwNotFoundException e) {
            error(httpServletResponse, HttpStatus.NOT_FOUND.value(), Code.validationException, e.getMessage());
            return;
        }

        if (!verifyProjectsExist(httpServletRequest, httpServletResponse, projects)) {
            return;
        }

        if (isInvalidAuthHeader(header)) {
            // check whether the uri allow anonymous in public project
            if (allowAnonymous(projects)) {
                // Build jwt token with anonymous user
                JwtLoginToken jwtLoginToken = new JwtLoginToken(null, "", List.of(
                        Role.builder().roleCode(Role.CODE_ANONYMOUS).roleName(Role.NAME_ANONYMOUS).build()));
                jwtLoginToken.setDetails(new WebAuthenticationDetails(httpServletRequest));
                SecurityContextHolder.getContext().setAuthentication(jwtLoginToken);
            } else {
                error(httpServletResponse, HttpStatus.UNAUTHORIZED.value(), Code.Unauthorized, "Not logged in.");
                return;
            }
        } else {
            String token = header.split(" ")[1].trim();
            Claims claims;
            try {
                claims = jwtTokenUtil.parseJwt(token);
                jwtClaimValidators.forEach(cv -> cv.validClaims(claims));
            } catch (SwValidationException e) {
                error(httpServletResponse, HttpStatus.UNAUTHORIZED.value(), Code.Unauthorized,
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
        var projectIds = (Set<String>) httpServletRequest.getAttribute(ProjectDetectionFilter.ATTRIBUTE_PROJECT);
        if (projectIds == null) {
            return Set.of();
        }

        return projectIds
                .stream()
                .map(projectService::findProject)
                .collect(Collectors.toSet());
    }

    private boolean isInvalidAuthHeader(String header) {
        return !StringUtils.hasText(header) || !header.startsWith("Bearer ");
    }

    private boolean verifyProjectsExist(HttpServletRequest request, HttpServletResponse response, Set<Project> projects)
            throws IOException {
        // never check for root path
        var uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) {
            return true;
        }
        if (projects.isEmpty()) {
            return true;
        }
        if (projects.stream().noneMatch(Project::isDeleted)) {
            return true;
        }
        if (WHITE_LIST_FOR_DELETED_PROJECTS.stream().anyMatch(p -> p.matcher(request.getRequestURI()).matches())) {
            return true;
        }
        error(response, HttpStatus.NOT_FOUND.value(), Code.validationException, "Project is deleted");
        return false;
    }
}
