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

package ai.starwhale.mlops.common;

import static ai.starwhale.mlops.common.proxy.ModelServing.MODEL_SERVICE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.proxy.Service;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProxyServletTest {
    private ProxyServlet proxyServlet;
    private Service mockService;

    @BeforeEach
    public void setUp() {
        mockService = mock(Service.class);
        var featuresProperties = new FeaturesProperties();
        proxyServlet = new ProxyServlet(featuresProperties, List.of(mockService));
    }

    @Test
    public void testGetTarget() {
        when(mockService.getPrefix()).thenReturn("foo");
        when(mockService.getTarget("bar/baz")).thenReturn("https://starwhale.ai/");

        // too short
        var thrown = assertThrows(IllegalArgumentException.class, () -> proxyServlet.getTarget("foo"));
        assertTrue(thrown.getMessage().startsWith("can not parse "));

        // no match service
        thrown = assertThrows(IllegalArgumentException.class, () -> proxyServlet.getTarget("bar/1/ppl"));
        assertTrue(thrown.getMessage().startsWith("can not find service for prefix "));

        var target = proxyServlet.getTarget("foo/bar/baz");
        assertTrue(target.startsWith("https://starwhale.ai/"));
    }

    @Test
    public void testService() throws ServletException, IOException {
        proxyServlet.init();

        var req = mock(HttpServletRequest.class);
        var uri = String.format("/%s/1/ppl", MODEL_SERVICE_PREFIX);
        when(req.getPathInfo()).thenReturn(uri);
        when(req.getMethod()).thenReturn("GET");
        var inputStream = mock(ServletInputStream.class);
        when(req.getInputStream()).thenReturn(inputStream);
        when(req.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));

        var resp = mock(HttpServletResponse.class);
        var outputStream = mock(ServletOutputStream.class);
        when(resp.getOutputStream()).thenReturn(outputStream);

        var servlet = spy(proxyServlet);
        doReturn("https://starwhale.ai/").when(servlet).getTarget(any());
        servlet.service(req, resp);
        verify(outputStream, atLeastOnce()).write(any(), anyInt(), intThat(len -> len > 0));
    }
}

