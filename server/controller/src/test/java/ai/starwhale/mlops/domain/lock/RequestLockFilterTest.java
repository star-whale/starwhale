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

package ai.starwhale.mlops.domain.lock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RequestLockFilterTest {

    private RequestLockFilter filter;
    private ControllerLockImpl lock;

    @BeforeEach
    public void setUp() {
        lock = new ControllerLockImpl();
        filter = new RequestLockFilter(lock);
    }


    @Test
    public void testFilter() throws Exception {
        HttpServletRequest get = new MockHttpServletRequest("GET", "/test");
        HttpServletRequest post = new MockHttpServletRequest("POST", "/test");
        HttpServletRequest put = new MockHttpServletRequest("PUT", "/test");
        HttpServletRequest delete = new MockHttpServletRequest("DELETE", "/test");
        AtomicBoolean callChain = new AtomicBoolean();
        FilterChain chain = mock(FilterChain.class);
        Mockito.doAnswer(a -> callChain.getAndSet(true))
                .when(chain)
                .doFilter(any(), any());

        testDoFilter(callChain, true, get, chain);
        testDoFilter(callChain, true, post, chain);
        testDoFilter(callChain, true, put, chain);
        testDoFilter(callChain, true, delete, chain);

        lock.lock(ControllerLock.TYPE_WRITE_REQUEST, "test11");
        testDoFilter(callChain, true, get, chain);
        testDoFilter(callChain, false, post, chain);
        testDoFilter(callChain, false, put, chain);
        testDoFilter(callChain, false, delete, chain);

        lock.unlock(ControllerLock.TYPE_WRITE_REQUEST, "test11");
        testDoFilter(callChain, true, get, chain);
        testDoFilter(callChain, true, post, chain);
        testDoFilter(callChain, true, put, chain);
        testDoFilter(callChain, true, delete, chain);
    }

    private void testDoFilter(AtomicBoolean callChain, boolean expect,
            HttpServletRequest req, FilterChain chain) throws Exception {
        HttpServletResponse resp = new MockHttpServletResponse();

        callChain.set(false);
        filter.doFilterInternal(req, resp, chain);
        if (expect) {
            Assertions.assertTrue(callChain.get());
        } else {
            Assertions.assertFalse(callChain.get());
        }
    }

}
