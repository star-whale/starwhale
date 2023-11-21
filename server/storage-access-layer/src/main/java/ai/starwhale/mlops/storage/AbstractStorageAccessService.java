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

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public abstract class AbstractStorageAccessService implements StorageAccessService {

    private final Set<String> schemas = new HashSet<>(
            StorageAccessService.Registry.getUriSchemasByClass(this.getClass()));

    public boolean compatibleWith(StorageUri uri) {
        return StringUtils.hasText(uri.getSchema()) && this.schemas.contains(uri.getSchema().toLowerCase());
    }
}
