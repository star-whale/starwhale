/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionMapper;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotSwdsHolder {

    Map<String, SWDSVersionWithMeta> swdsHolder;

    final SWDatasetVersionMapper swDatasetVersionMapper;

    final SWDSVersionWithMetaConverter swdsVersionWithMetaConverter;

    public HotSwdsHolder(
        SWDatasetVersionMapper swDatasetVersionMapper,
        SWDSVersionWithMetaConverter swdsVersionWithMetaConverter) {
        this.swDatasetVersionMapper = swDatasetVersionMapper;
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

    @PostConstruct
    public void loadUploadingDs() {
        List<SWDatasetVersionEntity> datasetVersionEntities = swDatasetVersionMapper.findVersionsByStatus(
            SWDatasetVersionEntity.STATUS_UN_AVAILABLE);
        datasetVersionEntities.parallelStream().forEach(this::manifest);
    }

}
