/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job.split;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.List;

/**
 * split job to tasks. One job shall not to be split multiple times
 */
public interface JobSpliterator {

    List<Task> split(Job job);
}
