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

package ai.starwhale.mlops.storage.domain;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

class DomainAwareStorageAccessServiceTest {

    DomainAwareStorageAccessService storageService;
    StorageAccessService delegated;

    @BeforeEach
    void setup() throws IOException {
        delegated = mock(StorageAccessService.class);
        when(delegated.signedUrl(eq("abc"), anyLong())).thenReturn("signed/abc");
        when(delegated.signedUrlAllDomains(eq("abc"), anyLong())).thenReturn(List.of(
                "http://aab.com/signed",
                "http://www.aab.com/signed",
                "http://10.2.3.4/signed",
                "http://10.2.3.4:8082/signed"
        ));
        when(delegated.signedPutUrl(eq("abc"), anyString(), anyLong())).thenReturn("signed/abc");
        when(delegated.signedPutUrlAllDomains(eq("abc"), anyString(), anyLong())).thenReturn(List.of(
                "http://aab.com/signed",
                "http://www.aab.com/signed",
                "http://10.2.3.4/signed",
                "http://10.2.3.4:8082/signed"
        ));
        storageService = new DomainAwareStorageAccessService(delegated);
    }

    @Test
    void signedUrl() throws IOException {
        RequestAttributes requestAttributes = mock(RequestAttributes.class);
        when(requestAttributes.getAttribute("SW_OSS_DOMAIN_REG_PATTERN", 0)).thenReturn(Pattern.compile("aab.com"));
        RequestContextHolder.setRequestAttributes(requestAttributes);
        Assertions.assertEquals(
                "http://aab.com/signed",
                storageService.signedUrl("abc", 12L)
        );
        when(requestAttributes.getAttribute("SW_OSS_DOMAIN_REG_PATTERN", 0)).thenReturn(null);
        Assertions.assertEquals(
                "signed/abc",
                storageService.signedUrl("abc", 12L)
        );
    }


    @Test
    void signedPutUrl() throws IOException {
        RequestAttributes requestAttributes = mock(RequestAttributes.class);
        when(requestAttributes.getAttribute("SW_OSS_DOMAIN_REG_PATTERN", 0)).thenReturn(Pattern.compile("aab.com"));
        RequestContextHolder.setRequestAttributes(requestAttributes);
        Assertions.assertEquals(
                "http://aab.com/signed",
                storageService.signedPutUrl("abc", "", 12L)
        );
        when(requestAttributes.getAttribute("SW_OSS_DOMAIN_REG_PATTERN", 0)).thenReturn(null);
        Assertions.assertEquals(
                "signed/abc",
                storageService.signedPutUrl("abc", "", 12L)
        );
    }

    @Test
    void signUrlWithoutPattern() throws IOException {
        Assertions.assertEquals("signed/abc", storageService.signUrl("abc", 12L, null));
    }

    @Test
    void signUrlWithPattern() throws IOException {
        Assertions.assertEquals(
                "http://aab.com/signed",
                storageService.signUrl("abc", 12L, Pattern.compile("aab.com"))
        );
        Assertions.assertEquals(
                "http://www.aab.com/signed",
                storageService.signUrl("abc", 12L, Pattern.compile("^www\\..*\\.com$"))
        );
        Assertions.assertEquals(
                "http://10.2.3.4/signed",
                storageService.signUrl("abc", 12L, Pattern.compile("^10.2.3.\\d+$"))
        );
        Assertions.assertEquals(
                "signed/abc",
                storageService.signUrl("abc", 12L, Pattern.compile("^doesnotmatchany"))
        );
    }

    @Test
    void signPutUrlWithoutPattern() throws IOException {
        Assertions.assertEquals("signed/abc", storageService.signPutUrl("abc", "", 12L, null));
    }

    @Test
    void signPutUrlWithPattern() throws IOException {
        Assertions.assertEquals(
                "http://aab.com/signed",
                storageService.signPutUrl("abc", "", 12L, Pattern.compile("aab.com"))
        );
        Assertions.assertEquals(
                "http://www.aab.com/signed",
                storageService.signPutUrl("abc", "", 12L, Pattern.compile("^www\\..*\\.com$"))
        );
        Assertions.assertEquals(
                "http://10.2.3.4/signed",
                storageService.signPutUrl("abc", "", 12L, Pattern.compile("^10.2.3.\\d+$"))
        );
        Assertions.assertEquals(
                "signed/abc",
                storageService.signPutUrl("abc", "", 12L, Pattern.compile("^doesnotmatchany"))
        );
    }
}