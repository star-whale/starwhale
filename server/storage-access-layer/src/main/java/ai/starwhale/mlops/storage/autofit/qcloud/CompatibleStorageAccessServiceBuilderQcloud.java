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

package ai.starwhale.mlops.storage.autofit.qcloud;

import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessService;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.autofit.s3.CompatibleStorageAccessServiceS3Like;
import ai.starwhale.mlops.storage.qcloud.StorageAccessServiceQcloud;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.util.Map;
import java.util.Set;

public class CompatibleStorageAccessServiceBuilderQcloud implements CompatibleStorageAccessServiceBuilder {

    public static final Set<String> TYPES = Set.of("tencent");

    @Override
    public CompatibleStorageAccessService build(Map<String, String> connectionToken) {
        S3Config s3Config = new S3Config(connectionToken);
        return new CompatibleStorageAccessServiceS3Like(new StorageAccessServiceQcloud(s3Config), s3Config, TYPES);
    }

    @Override
    public boolean couldBuild(String type) {
        return TYPES.contains(type.trim().toLowerCase());
    }
}
