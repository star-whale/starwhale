/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl;

import ai.starwhale.mlops.resulting.CollectorFinder;
import ai.starwhale.mlops.resulting.ResultCollector;
import java.util.Optional;

import ai.starwhale.mlops.resulting.impl.clsmulti.MCResultCollectorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * just return a MCResultCollector
 */
@Service
@Slf4j
public class NaiveCollectorFinder implements CollectorFinder {

    final MCResultCollectorFactory mcResultCollectorFactory;

    public NaiveCollectorFinder(MCResultCollectorFactory mcResultCollectorFactory) {
        this.mcResultCollectorFactory = mcResultCollectorFactory;
    }

    @Override
    public Optional<ResultCollector> findCollector(Long jobId) {
        return mcResultCollectorFactory.of(jobId.toString()).map(mcResultCollector -> (ResultCollector)mcResultCollector);
    }
}
