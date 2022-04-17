/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.init;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.agent.task.persistence.TaskPersistence;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.task.TaskStatus;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RebuildTasksAction implements DoTransition<Void, List<EvaluationTask>> {

    @Autowired
    private TaskPool taskPool;

    @Autowired
    private SourcePool sourcePool;

    @Autowired
    private TaskPersistence taskPersistence;

    @Override
    public boolean valid(Void v, Context context) {
        return !taskPool.isReady();
    }

    @Override
    public List<EvaluationTask> processing(Void v, Context context)
            throws Exception {
        log.info("start to rebuild task pool");
        List<EvaluationTask> tasks = taskPersistence.getAllActiveTasks().orElse(List.of());
        tasks.forEach(taskPool::fill);
        return tasks;
    }

    @Override
    public void success(Void v, List<EvaluationTask> tasks, Context context) {
        taskPool.setToReady();
        log.info("rebuild task pool success, size:{}", tasks.size());
    }

    @Override
    public void fail(Void v, Context context, Exception e) {
        log.info("rebuild task pool error:{}", e.getMessage(), e);
    }
}
