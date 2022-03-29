/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.init;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.agent.task.persistence.TaskPersistence;
import ai.starwhale.mlops.api.ReportApi;
import cn.hutool.core.collection.CollectionUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class RebuildTasksAction implements DoTransition<String, List<EvaluationTask>> {
    @Autowired
    private SourcePool sourcePool;

    @Autowired
    private TaskPool taskPool;

    private ContainerClient containerClient;

    private ReportApi reportApi;

    @Autowired
    private TaskPersistence taskPersistence;

    @Override
    public boolean valid(String basePath, Context context) {
        return StringUtils.hasText(basePath) && !taskPool.isReady();
    }

    @Override
    public List<EvaluationTask> processing(String basePath, Context context)
        throws Exception {
        log.info("start to rebuild task pool");
        return taskPersistence.getAll();
    }

    @Override
    public void success(String basePath, List<EvaluationTask> tasks, Context context) {
        if (CollectionUtil.isNotEmpty(tasks)) {
            tasks.forEach(taskPool::fill);
            taskPool.setToReady();
            log.info("rebuild task pool success, size:{}", tasks.size());
        }
    }

    @Override
    public void fail(String basePath, Context context, Exception e) {
        log.info("rebuild task pool from:{} error", basePath);
    }
}
