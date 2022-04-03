/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl;

import ai.starwhale.mlops.resulting.CollectorFinder;
import ai.starwhale.mlops.resulting.ResultCollector;
import ai.starwhale.mlops.resulting.impl.clsmulti.MCResultCollector;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * just return a MCResultCollector
 */
@Slf4j
public class NaiveCollectorFinder implements CollectorFinder {

    @Override
    public Optional<ResultCollector> findCollector(Long jobId) {
        try {
            return Optional.of(new MCResultCollector(jobId.toString()));
        } catch (IOException e) {
            log.error("initing MCResultCollector failed for job {}",jobId,e);
            return Optional.empty();
        }
    }
}
