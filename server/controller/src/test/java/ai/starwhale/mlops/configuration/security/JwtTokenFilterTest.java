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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.common.util.HttpUtil;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import io.jsonwebtoken.impl.DefaultClaims;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;

public class JwtTokenFilterTest {

    JwtTokenFilter jwtTokenFilter;
    JwtTokenUtil jwtTokenUtil;
    UserService userService;

    ProjectService projectService;
    List<JwtClaimValidator> jwtClaimValidators;

    MockedStatic<HttpUtil> httpUtilMockedStatic;

    @BeforeEach
    public void setup() {
        jwtTokenUtil = mock(JwtTokenUtil.class);
        when(jwtTokenUtil.parseJwt("y")).thenThrow(new SwValidationException(ValidSubject.USER));
        DefaultClaims claims = new DefaultClaims(Map.of("taskId", "x"));
        when(jwtTokenUtil.parseJwt("a")).thenReturn(claims);
        JwtClaimValidator jwtClaimValidator = mock(JwtClaimValidator.class);
        doThrow(SwValidationException.class).when(jwtClaimValidator).validClaims(claims);
        jwtClaimValidators = List.of(jwtClaimValidator);
        projectService = mock(ProjectService.class);
        jwtTokenFilter = new JwtTokenFilter(jwtTokenUtil, userService, projectService, jwtClaimValidators);
        httpUtilMockedStatic = mockStatic(HttpUtil.class);
    }

    @AfterEach
    public void destroy() {
        httpUtilMockedStatic.close();
    }

    @Test
    public void testUserTokenValidFailed() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer y");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        FilterChain filterchain = mock(FilterChain.class);
        jwtTokenFilter.doFilterInternal(request, response, filterchain);
        httpUtilMockedStatic.verify(() -> HttpUtil.error(response, HttpStatus.UNAUTHORIZED.value(), Code.accessDenied,
                "JWT token is expired or invalid."), times(1));
    }


    @Test
    public void testJobTokenValidFailed() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer a");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        FilterChain filterchain = mock(FilterChain.class);
        jwtTokenFilter.doFilterInternal(request, response, filterchain);
        httpUtilMockedStatic.verify(() -> HttpUtil.error(response, HttpStatus.UNAUTHORIZED.value(), Code.accessDenied,
                "JWT token is expired or invalid."), times(1));
    }

    @ParameterizedTest
    @CsvSource({"PUBLIC, 0", "PRIVATE, 1"})
    public void testAnonymous(Project.Privacy privacy, int errorTimes) throws ServletException, IOException {
        when(projectService.findProject("p-1"))
                .thenReturn(Project.builder().privacy(privacy).build());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getAttribute("PROJECT")).thenReturn(Set.of("p-1"));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        FilterChain filterchain = mock(FilterChain.class);
        jwtTokenFilter.doFilterInternal(request, response, filterchain);
        httpUtilMockedStatic.verify(() -> HttpUtil.error(response, HttpStatus.UNAUTHORIZED.value(), Code.Unauthorized,
                "Not logged in."), times(errorTimes));
    }

}
