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

package ai.starwhale.mlops.domain.upgrade.rollup.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.ControllerProperties;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletOutputStream;

/**
 * a test for RollingUpdateFilter
 */
public class RollingUpdateFilterTest {

    private ControllerProperties controllerProperties;

    private RollingUpdateFilter rollingUpdateFilter;

    @BeforeEach
    public void setup() {
        controllerProperties = mock(ControllerProperties.class);
        when(controllerProperties.getApiPrefix()).thenReturn("apf");
        rollingUpdateFilter = new RollingUpdateFilter(controllerProperties);
    }

    @Test
    public void testWhiteListLogin() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/login");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testWhiteListLogout() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/logout");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testWhiteListUserCurrent() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/user/current");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testWhiteListStatusNotify() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/system/upgrade/instance/status");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testBlockAtFirst() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/other");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(System.out));
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testOnNewInstanceStatusReadyUp() throws InterruptedException, IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/other");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(System.out));
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.onNewInstanceStatus(ServerInstanceStatus.READY_UP);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testOnNewInstanceStatusDown() throws InterruptedException, IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/other");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(System.out));
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.onNewInstanceStatus(ServerInstanceStatus.DOWN);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testOnOldInstanceStatusDown() throws InterruptedException, IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/apf/other");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(System.out));
        FilterChain filterChain = mock(FilterChain.class);
        rollingUpdateFilter.onOldInstanceStatus(ServerInstanceStatus.DOWN);
        rollingUpdateFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }


}

