/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

public class TaskPool {

        public final Queue<InferenceTask> preparingTasks = new ArrayDeque<>();
        public final List<InferenceTask> runningTasks = new Vector<>();
        public final List<InferenceTask> uploadingTasks = new Vector<>();
        public final List<InferenceTask> succeedTasks = new Vector<>();
        //public final List<InferenceTask> archivedTasks = new Vector<>();
        public final List<InferenceTask> canceledTasks = new Vector<>();
        public final List<InferenceTask> failedTasks = new Vector<>();
        public final List<Long> needToCancel = new Vector<>();

        public void fill(InferenceTask task) {
            switch (task.getStatus()) {
                case PREPARING:
                    preparingTasks.add(task);
                    break;
                case RUNNING:
                    runningTasks.add(task);
                    break;
                case UPLOADING:
                    uploadingTasks.add(task);
                    break;
                case SUCCESS:
                    succeedTasks.add(task);
                    break;
                case FAIL:
                    failedTasks.add(task);
                    break;
                case ARCHIVED:
                    //archivedTasks.add(task);
                    break;
                case CANCELING:
                    switch (task.getStage()) {
                        case PREPARING:
                            preparingTasks.add(task);
                            break;
                        case RUNNING:
                            runningTasks.add(task);
                            break;
                        case UPLOADING:
                            uploadingTasks.add(task);
                            break;
                    }
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
