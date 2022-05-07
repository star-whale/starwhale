/**
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

package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class HotSwdsHolder {

    Map<String, SWDSVersionWithMeta> swdsHolder;

    final SWDSVersionWithMetaConverter swdsVersionWithMetaConverter;

    public HotSwdsHolder(SWDSVersionWithMetaConverter swdsVersionWithMetaConverter) {
        this.swdsVersionWithMetaConverter = swdsVersionWithMetaConverter;
        this.swdsHolder = new ConcurrentHashMap<>();
    }

    public void manifest(SWDatasetVersionEntity swDatasetVersionEntity) {
        swdsHolder.put(swDatasetVersionEntity.getVersionName(),
            swdsVersionWithMetaConverter.from(swDatasetVersionEntity));
    }

    public void cancel(String swdsId) {
        swdsHolder.remove(swdsId);
    }

    public void end(String swdsId) {
        swdsHolder.remove(swdsId);
    }

    public Optional<SWDSVersionWithMeta> of(String id) {
        return Optional.ofNullable(swdsHolder.get(id));
    }

}
