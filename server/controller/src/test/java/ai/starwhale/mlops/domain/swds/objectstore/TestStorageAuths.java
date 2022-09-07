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

package ai.starwhale.mlops.domain.swds.objectstore;

import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import ai.starwhale.mlops.storage.fs.FileStorageEnv.FileSystemEnvType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestStorageAuths {

    @Test
    public void testS3(){
        final String auths="USER.S3.REGION=region\n"
            + "USER.S3.ENDPOINT=endpoint\n"
            + "USER.S3.SECRET=secret\n"
            + "USER.S3.ACCESS_KEY=access_key\n"
            + "USER.S3.myname.ENDPOINT=endpoint1\n"
            + "USER.S3.myname.SECRET=secret1\n"
            + "USER.S3.MNIST.SECRET=\n"
            + "USER.S3.myname.ACCESS_KEY=access_key1\n";
        StorageAuths storageAuths = new StorageAuths(auths);
        FileStorageEnv defaultEnv = storageAuths.getEnv("");
        Assertions.assertEquals(FileSystemEnvType.S3, defaultEnv.getEnvType());
        Assertions.assertEquals("region", defaultEnv.getEnvs().get("USER.S3.REGION"));
        Assertions.assertEquals("endpoint", defaultEnv.getEnvs().get("USER.S3.ENDPOINT"));
        Assertions.assertEquals("secret", defaultEnv.getEnvs().get("USER.S3.SECRET"));
        Assertions.assertEquals("access_key", defaultEnv.getEnvs().get("USER.S3.ACCESS_KEY"));

        FileStorageEnv myEnv = storageAuths.getEnv("myname");
        Assertions.assertEquals(FileSystemEnvType.S3, myEnv.getEnvType());
        Assertions.assertNull(myEnv.getEnvs().get("USER.S3.myname.REGION"));
        Assertions.assertEquals("endpoint1", myEnv.getEnvs().get("USER.S3.MYNAME.ENDPOINT"));
        Assertions.assertEquals("secret1", myEnv.getEnvs().get("USER.S3.MYNAME.SECRET"));
        Assertions.assertEquals("access_key1", myEnv.getEnvs().get("USER.S3.MYNAME.ACCESS_KEY"));

        FileStorageEnv mnist = storageAuths.getEnv("MNIST");
        Assertions.assertEquals(FileSystemEnvType.S3, mnist.getEnvType());
        Assertions.assertEquals("", mnist.getEnvs().get("USER.S3.MNIST.SECRET"));
    }

}
