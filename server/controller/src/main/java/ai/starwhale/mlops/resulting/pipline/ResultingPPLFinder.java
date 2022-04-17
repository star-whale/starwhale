package ai.starwhale.mlops.resulting.pipline;

import ai.starwhale.mlops.domain.job.Job;

/**
 * build a resulting pipeline for one job
 */
public interface ResultingPPLFinder {

    ResultingPPL findForJob(Job job);

}
