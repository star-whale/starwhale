/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

@Service
public class TaskPool {

        public final Queue<EvaluationTask> preparingTasks = new ArrayDeque<>();
        public final List<EvaluationTask> runningTasks = new Vector<>();
        public final List<EvaluationTask> uploadingTasks = new Vector<>();
        public final List<EvaluationTask> finishedTasks = new Vector<>();
        public final List<EvaluationTask> archivedTasks = new Vector<>();
        public final List<EvaluationTask> canceledTasks = new Vector<>();
        public final List<EvaluationTask> errorTasks = new Vector<>();
        public final List<Long> needToCancel = new Vector<>();

        public void fill(EvaluationTask task) {
            switch (task.getStatus()) {
                case CREATED:
                    break;
                case PREPARING:
                    preparingTasks.add(task);
                    break;
                case RUNNING:
                    runningTasks.add(task);
                    break;
                case UPLOADING:
                    uploadingTasks.add(task);
                    break;
                case FINISHED:
                    finishedTasks.add(task);
                    break;
                case ARCHIVED:
                    archivedTasks.add(task);
                    break;
                case EXIT_ERROR:
                    errorTasks.add(task);
                    break;
                case CANCELED:
                    canceledTasks.add(task);
            }
        }

        /**
         * whether init successfully
         */
        private volatile boolean ready = false;

        public boolean isReady() {
            return ready;
        }

        public void setToReady() {
            ready = true;
        }
    }
