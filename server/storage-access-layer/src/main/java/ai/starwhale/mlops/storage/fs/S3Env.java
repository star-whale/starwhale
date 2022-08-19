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

/**
 * holds s3 env keys
 */
public class S3Env extends FileStorageEnv {

    public static final String ENV_BUCKET="SW_S3_BUCKET";

    public static final String ENV_SECRET_KEY="SW_S3_SECRET";

    public static final String ENV_SECRET_ID="SW_S3_ACCESS_KEY";

    public static final String ENV_REGION="SW_S3_REGION";

    public static final String ENV_ENDPOINT="SW_S3_ENDPOINT";

    public S3Env() {
        super(FileSystemEnvType.S3);
    }
}
