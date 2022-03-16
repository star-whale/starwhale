/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.task.Task;

import java.util.List;

/**
 * split job to tasks. One job shall not to be split multiple times
 */
public interface JobSpliter {

    List<Task> split(Job job);
}
