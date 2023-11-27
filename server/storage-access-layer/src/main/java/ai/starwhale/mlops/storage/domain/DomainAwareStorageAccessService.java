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

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
public class DomainAwareStorageAccessService implements StorageAccessService {

    public static final String OSS_DOMAIN_PATTERN_ATTR = "SW_OSS_DOMAIN_REG_PATTERN";

    private interface Excludes {
        String signedUrl(String path, Long expTimeMillis);

        String signedPutUrl(String path, String contentType, Long expTimeMillis);
    }

    @Delegate(excludes = Excludes.class)
    final StorageAccessService delegated;

    public DomainAwareStorageAccessService(StorageAccessService storageAccessService) {
        this.delegated = storageAccessService;
    }

    public String signedUrl(String path, Long expTimeMillis) throws IOException {
        return signUrl(path, expTimeMillis, domainPatternFromWeb());
    }

    public String signedPutUrl(String path, String contentType, Long expTimeMillis) throws IOException {
        return signPutUrl(path, contentType, expTimeMillis, domainPatternFromWeb());
    }

    public String signUrl(String path, Long expTimeMillis, Pattern domainPattern) throws IOException {
        if (null == domainPattern) {
            return delegated.signedUrl(path, expTimeMillis);
        }
        List<String> urls = delegated.signedUrlAllDomains(path, expTimeMillis);
        String url = urlFirstMatch(domainPattern, urls);
        if (url != null) {
            return url;
        }
        return delegated.signedUrl(path, expTimeMillis);
    }

    @Nullable
    private static String urlFirstMatch(Pattern domainPattern, List<String> urls) {
        for (var url : urls) {
            URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                log.warn("signed url from StorageAccessService is not valid uri {}", url);
                continue;
            }
            String host = uri.getHost();
            if (null == host) {
                continue;
            }
            if (domainPattern.matcher(host).matches()) {
                return url;
            }
        }
        return null;
    }

    public String signPutUrl(String path, String contentType, Long expTimeMillis, Pattern domainPattern)
            throws IOException {
        if (null == domainPattern) {
            return delegated.signedPutUrl(path, contentType, expTimeMillis);
        }
        List<String> urls = delegated.signedPutUrlAllDomains(path, contentType, expTimeMillis);
        String url = urlFirstMatch(domainPattern, urls);
        if (url != null) {
            return url;
        }
        return delegated.signedPutUrl(path, contentType, expTimeMillis);
    }

    private Pattern domainPatternFromWeb() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object possibleDomainPattern = attrs.getAttribute(OSS_DOMAIN_PATTERN_ATTR, RequestAttributes.SCOPE_REQUEST);
        if (null == possibleDomainPattern || !(possibleDomainPattern instanceof Pattern)) {
            return null;
        }
        return (Pattern) possibleDomainPattern;
    }

    public StorageAccessService getDelegated() {
        return this.delegated;
    }
}
