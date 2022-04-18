/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.resulting.impl.clsmulti.ppl.MCResultingPPL;
import ai.starwhale.mlops.resulting.pipline.ResultingPPL;
import ai.starwhale.mlops.resulting.pipline.ResultingPPLFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * just return a McResultCalculator
 */
@Service
@Slf4j
public class NaiveResultingPPLFinder implements ResultingPPLFinder {

    final MCResultingPPL mcResultingPPL;

    public NaiveResultingPPLFinder(
        MCResultingPPL mcResultingPPL) {
        this.mcResultingPPL = mcResultingPPL;
    }

    @Override
    public ResultingPPL findForJob(Job job) {
        return mcResultingPPL;
    }
}
