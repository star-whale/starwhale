/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HotSwdsLoader implements CommandLineRunner {

    final SWDatasetVersionMapper swDatasetVersionMapper;

    final HotSwdsHolder hotSwdsHolder;

    public HotSwdsLoader(
        SWDatasetVersionMapper swDatasetVersionMapper,
        HotSwdsHolder hotSwdsHolder) {
        this.swDatasetVersionMapper = swDatasetVersionMapper;
        this.hotSwdsHolder = hotSwdsHolder;
    }

    @Override
    public void run(String... args) throws Exception {
        loadUploadingDs();
    }

    void loadUploadingDs() {
        List<SWDatasetVersionEntity> datasetVersionEntities = swDatasetVersionMapper.findVersionsByStatus(
            SWDatasetVersionEntity.STATUS_UN_AVAILABLE);
        datasetVersionEntities.parallelStream().forEach(entity -> hotSwdsHolder.manifest(entity));
    }
}
