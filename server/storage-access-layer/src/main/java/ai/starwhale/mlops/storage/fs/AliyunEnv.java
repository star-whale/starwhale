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

public class AliyunEnv extends S3Env {
    public static final String ENV_EXTRA_S3_CONFIGS = "SW_S3_EXTRA_CONFIGS";

    public AliyunEnv() {
        super(FileSystemEnvType.ALIYUN);
    }

    public void setExtraS3Configs(String extraS3Configs) {
        this.add(ENV_EXTRA_S3_CONFIGS, extraS3Configs);
    }
}
