/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.resulting.impl.clsmulti.repo.McIndicatorRepo;
import ai.starwhale.mlops.resulting.repo.IndicatorRepo;
import ai.starwhale.mlops.resulting.repo.IndicatorRepoFinder;
import org.springframework.stereotype.Component;

@Component
public class NaiveIndicatorRepoFinder implements IndicatorRepoFinder {

    final McIndicatorRepo mcIndicatorRepo;

    public NaiveIndicatorRepoFinder(
        McIndicatorRepo mcIndicatorRepo) {
        this.mcIndicatorRepo = mcIndicatorRepo;
    }

    @Override
    public IndicatorRepo find(Job job) {
        return mcIndicatorRepo;
    }
}
