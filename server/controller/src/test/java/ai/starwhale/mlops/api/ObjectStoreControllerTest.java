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

package ai.starwhale.mlops.api;

import static ai.starwhale.mlops.api.ObjectStoreController.URI_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.DelegatingServletOutputStream;

public class ObjectStoreControllerTest {

    private ObjectStoreController objectStoreController;

    private StorageAccessService storageAccessService;

    private HttpServletResponse rsp;
    private HttpServletRequest req;
    private DelegatingServletOutputStream outputStream;

    @BeforeEach
    public void setUp() throws IOException {
        storageAccessService = mock(StorageAccessService.class);
        objectStoreController = new ObjectStoreController(storageAccessService);
        rsp = mock(HttpServletResponse.class);
        outputStream = new DelegatingServletOutputStream(new ByteArrayOutputStream());
        when(rsp.getOutputStream()).thenReturn(outputStream);
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(
                "/" + URI_PREFIX + "/p/" + System.currentTimeMillis() + 1000);
        when(req.getContextPath()).thenReturn("");
    }

    @Test
    public void testWithRange() throws IOException {
        var p = "p";
        var r = "bytes=0-100";
        String content = "content";
        LengthAbleInputStream inputStream = new LengthAbleInputStream(new ByteArrayInputStream(content.getBytes()),
                content.length());
        when(storageAccessService.get(p, 0L, 101L)).thenReturn(inputStream);
        objectStoreController.getObjectContent(r, req, rsp);
        Assertions.assertEquals(content, outputStream.getTargetStream().toString());

    }

    @Test
    public void testWithOutRange() throws IOException {
        var p = "p";
        String r = null;
        String content = "content";
        LengthAbleInputStream inputStream = new LengthAbleInputStream(new ByteArrayInputStream(content.getBytes()),
                content.length());
        when(storageAccessService.get(p)).thenReturn(inputStream);
        objectStoreController.getObjectContent(r, req, rsp);
        Assertions.assertEquals(content, outputStream.getTargetStream().toString());

    }

    @Test
    public void testWithOutRangeExcept() throws IOException {
        var p = "p";
        var r = "asdfa";
        String content = "content";
        LengthAbleInputStream inputStream = new LengthAbleInputStream(new ByteArrayInputStream(content.getBytes()),
                content.length());
        when(storageAccessService.get(p)).thenReturn(inputStream);
        objectStoreController.getObjectContent(r, req, rsp);
        Assertions.assertEquals(content, outputStream.getTargetStream().toString());

    }

    @Test
    public void testPut() throws IOException {
        var p = "p";
        String content = "content";
        ServletInputStream inputStream = new DelegatingServletInputStream(new ByteArrayInputStream(content.getBytes()));
        when(req.getInputStream()).thenReturn(inputStream);
        objectStoreController.modifyObjectContent(req);
        verify(storageAccessService).put(p, inputStream);
    }

}
