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

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.concurrent.Future;

/**
 * schedule tasks of jobs
 * Spring framework has a class named TaskScheduler with a bean in the context. To avoid bean conflict the name here is
 * SwTaskScheduler
 */
public interface SwTaskScheduler {


    /**
     * @param task the task to be scheduled
     */
    void schedule(Task task);

    /**
     * @param task task to be stopped
     */
    void stop(Task task);

    /**
     * @param task the tasks to exec on
     * @param command command may be wrapped with "sh -c" by implementations
     * @return stdout, stderr in String[]
     */
    Future<String[]> exec(Task task, String... command);
}
