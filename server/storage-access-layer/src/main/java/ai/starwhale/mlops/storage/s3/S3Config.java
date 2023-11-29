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

package ai.starwhale.mlops.storage.s3;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.StringUtils;

@Slf4j
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode
public class S3Config {

    private String bucket;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
    private List<String> endpointEquivalents;
    private String endpointEquivalentsRaw;
    private Map<String, String> endpointEquivalentsMap;
    private long hugeFileThreshold;
    private long hugeFilePartSize;

    public S3Config(Map<String, String> tokens) throws IOException {
        this.bucket = tokens.get("bucket");
        this.accessKey = tokens.get("ak");
        this.secretKey = tokens.get("sk");
        this.region = tokens.get("region");
        this.endpoint = tokens.get("endpoint");
        setEndpointEquivalentsRaw(tokens.get("endpointEquivalents"));
        try {
            this.hugeFileThreshold = Long.parseLong(tokens.get("hugeFileThreshold"));
        } catch (Exception e) {
            log.error("failed to parse hugeFileThreshold", e);
        }
        try {
            this.hugeFilePartSize = Long.parseLong(tokens.get("hugeFilePartSize"));
        } catch (Exception e) {
            log.error("failed to parse hugeFilePartSize", e);
        }
    }

    public URL getEndpointUrl() {
        try {
            return endpointToUrl(this.endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL endpointToUrl(String endpoint) throws MalformedURLException {
        if (endpoint.contains("://")) {
            return new URL(endpoint);
        } else {
            return new URL("http://" + endpoint);
        }
    }

    public void setEndpointEquivalentsRaw(String endpointEquivalentsRaw) throws IOException {
        this.endpointEquivalentsRaw = endpointEquivalentsRaw;
        if (!StringUtils.hasText(endpointEquivalentsRaw)) {
            this.endpointEquivalentsMap = Map.of();
            this.endpointEquivalents = List.of();
        } else {
            this.endpointEquivalentsMap = new ObjectMapper().readValue(endpointEquivalentsRaw, Map.class);
            this.endpointEquivalents = this.endpointEquivalentsMap.values().stream().collect(Collectors.toList());
        }
    }
}
