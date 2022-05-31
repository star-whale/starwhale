/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.agent.task.inferencetask;

import java.util.*;

public class TaskPool {

    public final Set<InferenceTask> preparingTasks = new LinkedHashSet<>();
    public final Set<InferenceTask> runningTasks = new LinkedHashSet<>();
    public final Set<InferenceTask> uploadingTasks = new LinkedHashSet<>();
    public final Set<InferenceTask> succeedTasks = new LinkedHashSet<>();
    public final Set<InferenceTask> canceledTasks = new LinkedHashSet<>();
    public final Set<InferenceTask> failedTasks = new LinkedHashSet<>();
    public final Set<Long> needToCancel = new LinkedHashSet<>();

    public void add2PreparingQueue(InferenceTask task) {
        preparingTasks.add(task);
    }


    public void fill(InferenceTask task) {
        switch (task.getStatus()) {
            case PREPARING:
                add2PreparingQueue(task);
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
                        add2PreparingQueue(task);
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
