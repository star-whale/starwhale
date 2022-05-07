/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
