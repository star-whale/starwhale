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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * s3://username:password@127.0.0.1:29000@starwhale/project/2/dataset/11/x
 * s3://localhsot@starwhale/project/2/dataset/11/s
 * s3://starwhale/project/2/dataset/11/s
 * /starwhale/project/2/dataset/11/d
 * starwhale/project/2/dataset/11/ab
 */
@Getter
public class StorageUri {

    /**
     * file/s3/ftp/nfs/oss/http/
     */
    String schema;
    String username;
    String password;
    String host;
    Integer port;
    String path;
    String bucket;

    String prefixWithoutPath;

    static final Pattern URI_PATTERN = Pattern.compile(
            "^((s3|file|ftp|nfs|oss|http|https|sftp)://)?"
                    + "(([a-zA-Z0-9]+:[a-zA-Z0-9]+)@)?"
                    + "(([a-z0-9.]+)(:(\\d{2,5}))?@)?"
                    + "(/?([^/]+)/(.*?([^/]+)/?))$");

    public StorageUri(String uri) {
        Matcher matcher = URI_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            this.path = uri;
            return;
        }
        schema = matcher.group(2);
        String up = matcher.group(4);
        if (null != up) {
            String[] split = up.split(":");
            username = split[0];
            password = split[1];
        }
        host = matcher.group(6);
        if (null != matcher.group(8)) {
            port = Integer.valueOf(matcher.group(8));
        }
        bucket = matcher.group(10);
        path = matcher.group(11);
        prefixWithoutPath = uri.replace(path, "");
    }

}
