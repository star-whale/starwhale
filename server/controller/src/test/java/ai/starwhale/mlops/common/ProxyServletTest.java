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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProxyServletTest {
    private ProxyServlet proxyServlet;
    private ModelServingMapper modelServingMapper;

    @BeforeEach
    public void setUp() {
        modelServingMapper = mock(ModelServingMapper.class);
        proxyServlet = new ProxyServlet(modelServingMapper);
    }

    @Test
    public void testGetTarget() {
        long id = 1L;
        when(modelServingMapper.find(id)).thenReturn(ModelServingEntity.builder().build());

        var uri = String.format("/%s/%d/ppl", ProxyServlet.MODEL_SERVICE_PREFIX, id);
        var rt = proxyServlet.getTarget(uri);
        Assertions.assertEquals("http://model-serving-1/ppl", rt);

        var tooShort = ProxyServlet.MODEL_SERVICE_PREFIX + "/1";
        Assertions.assertThrows(IllegalArgumentException.class, () -> proxyServlet.getTarget(tooShort));

        var wrongStartsWith = "/foo/1/ppl";
        Assertions.assertThrows(IllegalArgumentException.class, () -> proxyServlet.getTarget(wrongStartsWith));

        var notFound = String.format("/%s/%d/ppl", ProxyServlet.MODEL_SERVICE_PREFIX, id + 1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> proxyServlet.getTarget(notFound));
    }

    @Test
    public void testService() throws ServletException, IOException {
        proxyServlet.init();

        var req = mock(HttpServletRequest.class);
        var uri = String.format("/%s/1/ppl", ProxyServlet.MODEL_SERVICE_PREFIX);
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

