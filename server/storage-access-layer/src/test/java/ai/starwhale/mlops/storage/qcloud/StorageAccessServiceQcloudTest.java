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

package ai.starwhale.mlops.storage.qcloud;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.storage.s3.S3Config;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class StorageAccessServiceQcloudTest {

    @Test
    // test using real qcloud account
    @Disabled
    public void testBasic() throws IOException, NoSuchAlgorithmException {
        var qcloud = new StorageAccessServiceQcloud(S3Config.builder()
                .bucket("bucket")
                .accessKey("ak")
                .secretKey("sk")
                .region("ap-beijing")
                .hugeFilePartSize(5 * 1024 * 1024)
                .build());
        String path = "qcloud/foo";
        var content = new byte[]{'b', 'a', 'r'};
        qcloud.put(path, content);
        var gets = qcloud.get(path);
        assertArrayEquals(content, gets.readAllBytes());

        var head = qcloud.head(path);
        assertEquals(content.length, head.getContentLength());

        head = qcloud.head(path, true);
        var md5sum = MessageDigest.getInstance("MD5").digest(content);
        // md5sum to hex
        var md5sumHex = String.format("%032x", new BigInteger(1, md5sum));
        assertEquals(head.getMd5sum(), md5sumHex);

        // put using input stream
        var content2 = new byte[]{'b', 'a', 'z'};
        var is = new ByteArrayInputStream(content2);
        var path2 = "qcloud/foo2";
        qcloud.put(path2, is);
        var gets2 = qcloud.get(path2);
        assertArrayEquals(content2, gets2.readAllBytes());

        var list = qcloud.list("qcloud");
        assertThat(list.collect(Collectors.toList()), containsInAnyOrder("qcloud/foo", "qcloud/foo2", "qcloud/"));


        qcloud.delete(path);
        list = qcloud.list("qcloud/");
        assertThat(list.collect(Collectors.toList()), containsInAnyOrder("qcloud/foo2", "qcloud/"));

        qcloud.delete(path2);
        list = qcloud.list("qcloud/");
        assertThat(list.collect(Collectors.toList()), containsInAnyOrder("qcloud/"));
    }
}
