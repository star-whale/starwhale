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


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class S3Config {

    private String bucket;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;

    private List<String> endpointEquivalents;
    private long hugeFileThreshold;
    private long hugeFilePartSize;

    public S3Config(Map<String, String> tokens) {
        this.bucket = tokens.get("bucket");
        this.accessKey = tokens.get("ak");
        this.secretKey = tokens.get("sk");
        this.region = tokens.get("region");
        this.endpoint = tokens.get("endpoint");
        String endpointEquivalentsRaw = tokens.get("endpointEquivalents");
        this.endpointEquivalents = StringUtils.hasText(endpointEquivalentsRaw)
                ? Arrays.asList(endpointEquivalentsRaw.split(",")) : null;
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
}
