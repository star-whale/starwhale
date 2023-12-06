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

import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.domain.DomainAwareStorageAccessService;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ObjectStoreDomainDetectionFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "SW_CLIENT_FAVORED_OSS_DOMAIN_ALIAS";

    final Map<String, Pattern> domainAliasMap;

    public ObjectStoreDomainDetectionFilter(StorageProperties storageProperties) {
        S3Config s3Config = storageProperties.getS3Config();
        if (null == s3Config) {
            domainAliasMap = new HashMap<>();
            return;
        }
        Map<String, String> endpointEquivalentsMap = s3Config.getEndpointEquivalentsMap();
        domainAliasMap = endpointEquivalentsMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey, (entry) -> {
            URI uri = null;
            try {
                uri = new URI(entry.getValue());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return Pattern.compile(uri.getHost().replace(".", "\\."));
        }));

    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Pattern hostPattern = domainAliasMap.get(request.getHeader(HEADER_NAME));
        if (null != hostPattern) {
            request.setAttribute(DomainAwareStorageAccessService.OSS_DOMAIN_PATTERN_ATTR, hostPattern);
        }
        filterChain.doFilter(request, response);
    }

}
