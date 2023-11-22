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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * s3://username:password@127.0.0.1:29000/starwhale/project/2/dataset/11/x
 * s3://localhsot/starwhale/project/2/dataset/11/s
 * s3://starwhale/project/2/dataset/11/s
 * /starwhale/project/2/dataset/11/d
 * starwhale/project/2/dataset/11/ab
 */
@Getter
public class StorageUri {

    static final Pattern PATH_PATTERN = Pattern.compile(
            "^(\\/?([^\\/]+)((\\/(([^\\/]+?)\\/?))*))$");
    String uriString;
    URI uri;
    /**
     * file/s3/ftp/nfs/oss/http/
     */
    String schema;
    String username;
    String password;
    String host;
    Integer port;
    String pathAfterBucket;
    String bucket;
    String path;
    String prefixWithBucket;

    public StorageUri(String u) throws URISyntaxException {
        this.uriString = u;
        this.uri = new URI(u);
        schema = uri.getScheme();
        path = uri.getPath();
        if (null == path) {
            throw new URISyntaxException(u, "path is null", 0);
        }
        String up = uri.getUserInfo();
        if (null != up) {
            String[] split = up.split(":");
            username = split[0];
            password = split[1];
        }
        host = uri.getHost();
        port = uri.getPort();
        if (port == -1) {
            port = null;
        }

        if (null == schema) {
            bucket = null;
            pathAfterBucket = path;
            prefixWithBucket = null;
        } else {
            Matcher matcher = PATH_PATTERN.matcher(path);
            if (matcher.matches()) {
                String group3 = matcher.group(3);
                bucket = StringUtils.hasText(group3) ? matcher.group(2) : null;
                pathAfterBucket = StringUtils.hasText(group3) ? group3 : path;
                prefixWithBucket = StringUtils.hasText(group3) ? u.replace(pathAfterBucket, "") : path;
            }
        }


    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageUri that = (StorageUri) o;
        return this.uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return this.uriString;
    }
}
