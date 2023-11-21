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

package ai.starwhale.mlops.storage;

import ai.starwhale.mlops.storage.s3.S3Config;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class S3LikeStorageAccessService extends AbstractStorageAccessService {

    protected final String endpoint;

    protected final String bucket;

    protected final long partSize;

    protected final long hugeFileThreshold;

    public S3LikeStorageAccessService(S3Config config) {
        this.endpoint = config.getEndpoint();
        this.bucket = config.getBucket();
        this.partSize = config.getHugeFilePartSize();
        this.hugeFileThreshold = config.getHugeFileThreshold();
    }

    public boolean compatibleWith(StorageUri uri) {
        if (!super.compatibleWith(uri)) {
            return false;
        }
        if (!this.bucket.equals(uri.getBucket())) {
            return false;
        }
        URI endpointUri;
        try {
            endpointUri = new URI(this.endpoint);
        } catch (URISyntaxException e) {
            log.error("s3 config error invalid endpoint {}", this.endpoint, e);
            return false;
        }
        if (!endpointUri.getHost().equals(uri.getHost())) {
            return false;
        }
        if (null != uri.getPort()) {
            return endpointUri.getPort() == uri.getPort();
        }
        return true;
    }
}
