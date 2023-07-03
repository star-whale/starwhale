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


import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class S3Config {

    private String bucket;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
    private long hugeFileThreshold;
    private long hugeFilePartSize;

    public S3Config(Map<String, String> tokens) {
        this.bucket = tokens.get("bucket");
        this.accessKey = tokens.get("ak");
        this.secretKey = tokens.get("sk");
        this.endpoint = tokens.get("endpoint");
        this.region = tokens.get("region");
        this.hugeFileThreshold = Long.parseLong(tokens.get("hugeFileThreshold"));
        this.hugeFilePartSize = Long.parseLong(tokens.get("hugeFilePartSize"));
    }

    public boolean overWriteEndPoint() {
        return null != endpoint && !endpoint.isBlank();
    }

}
