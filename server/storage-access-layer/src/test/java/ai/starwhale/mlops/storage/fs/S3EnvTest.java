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

package ai.starwhale.mlops.storage.fs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class S3EnvTest {
    @Test
    public void testSet() {
        var s3Env = new S3Env();
        assertThat(s3Env.getEnvType(), is(FileStorageEnv.FileSystemEnvType.S3));
        var envValue = randomString();
        s3Env.setEndPoint(envValue);
        assertThat(mapContains(s3Env.getEnvs(), S3Env.ENV_ENDPOINT, envValue), is(true));

        envValue = randomString();
        s3Env.setBucket(envValue);
        assertThat(mapContains(s3Env.getEnvs(), S3Env.ENV_BUCKET, envValue), is(true));

        envValue = randomString();
        s3Env.setAccessKey(envValue);
        assertThat(mapContains(s3Env.getEnvs(), S3Env.ENV_SECRET_ID, envValue), is(true));

        envValue = randomString();
        s3Env.setSecret(envValue);
        assertThat(mapContains(s3Env.getEnvs(), S3Env.ENV_SECRET_KEY, envValue), is(true));

        envValue = randomString();
        s3Env.setRegion(envValue);
        assertThat(mapContains(s3Env.getEnvs(), S3Env.ENV_REGION, envValue), is(true));
    }

    public String randomString() {
        return UUID.randomUUID().toString();
    }

    public boolean mapContains(Map<String, String> map, String key, String val) {
        return map.containsKey(key) && map.get(key).equals(val);
    }
}
