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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ObjectStoreDomainDetectionFilterTest {

    ObjectStoreDomainDetectionFilter filter;

    @BeforeEach
    void setup() throws IOException {
        StorageProperties storageProperties = mock(StorageProperties.class);
        when(storageProperties.getS3Config()).thenReturn(new S3Config(
                Map.of("endpointEquivalents", "{\"test1\":\"http://120.0.1.2\", \"test2\":\"http://a.b.com\"}"))
        );
        filter = new ObjectStoreDomainDetectionFilter(storageProperties);
    }

    @Test
    void doFilterInternal() throws ServletException, IOException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(req.getHeader("SW_CLIENT_FAVORED_OSS_DOMAIN_ALIAS")).thenReturn(null);
        filter.doFilterInternal(req, resp, filterChain);
        verify(req, times(0)).setAttribute(any(), any());
        when(req.getHeader("SW_CLIENT_FAVORED_OSS_DOMAIN_ALIAS")).thenReturn("test1");
        filter.doFilterInternal(req, resp, filterChain);
        ArgumentCaptor<Pattern> ac = ArgumentCaptor.forClass(Pattern.class);
        verify(req).setAttribute(eq("SW_OSS_DOMAIN_REG_PATTERN"), ac.capture());
        Assertions.assertTrue(ac.getValue().matcher("120.0.1.2").matches());
        Assertions.assertFalse(ac.getValue().matcher("120a0.1.2").matches());
    }
}