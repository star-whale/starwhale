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

package ai.starwhale.mlops.api.config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.security.ContentCachingFilter;
import ai.starwhale.mlops.configuration.security.ContentCachingFilter.CachedBodyHttpServletRequest;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ContentCachingFilterTest {

    ContentCachingFilter contentCachingFilter = new ContentCachingFilter("/api/v1");

    @Test
    public void testNonDatastore() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/ax");
        contentCachingFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(eq(request), eq(response));
    }

    @Test
    public void testDatastore() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/datastore/xx");
        contentCachingFilter.doFilter(request, response, filterChain);
        ArgumentCaptor<HttpServletRequest> arg = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(arg.capture(), eq(response));
        Assertions.assertEquals(CachedBodyHttpServletRequest.class, arg.getValue().getClass());

    }

}
