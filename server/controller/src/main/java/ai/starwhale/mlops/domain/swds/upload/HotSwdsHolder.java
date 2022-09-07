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

package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.upload.bo.SwdsVersionWithMeta;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class HotSwdsHolder {

    Map<String, SwdsVersionWithMeta> swdsHolder;

    final SwdsVersionWithMetaConverter swdsVersionWithMetaConverter;

    public HotSwdsHolder(SwdsVersionWithMetaConverter swdsVersionWithMetaConverter) {
        this.swdsVersionWithMetaConverter = swdsVersionWithMetaConverter;
        this.swdsHolder = new ConcurrentHashMap<>();
    }

    public void manifest(SwDatasetVersionEntity swDatasetVersionEntity) {
        swdsHolder.put(swDatasetVersionEntity.getVersionName(),
                swdsVersionWithMetaConverter.from(swDatasetVersionEntity));
    }

    public void cancel(String swdsId) {
        swdsHolder.remove(swdsId);
    }

    public void end(String swdsId) {
        swdsHolder.remove(swdsId);
    }

    public Optional<SwdsVersionWithMeta> of(String id) {
        return Optional.ofNullable(swdsHolder.get(id));
    }

}
