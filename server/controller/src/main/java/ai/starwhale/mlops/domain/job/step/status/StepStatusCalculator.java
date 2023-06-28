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

package ai.starwhale.mlops.domain.job.step.status;

import ai.starwhale.mlops.domain.job.StatusRequirement;
import ai.starwhale.mlops.domain.job.StatusRequirement.RequireType;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepStatusCalculator {
    /**
     * set of TRs are and relationship
     */
    static final Map<StepStatus, Set<StatusRequirement<TaskStatus>>> stepStatusRequirementSetMap;

    static {
        Map<StepStatus, Set<StatusRequirement<TaskStatus>>> map = new LinkedHashMap<>();
        map.put(StepStatus.RUNNING, Set.of(
                new StatusRequirement<>(
                    Set.of(TaskStatus.PREPARING, TaskStatus.RUNNING, TaskStatus.READY),
                    RequireType.MUST),
                new StatusRequirement<>(
                    Set.of(TaskStatus.PAUSED, TaskStatus.CANCELLING,
                    TaskStatus.CANCELED, TaskStatus.FAIL), RequireType.HAVE_NO)));
        map.put(StepStatus.SUCCESS,
                Set.of(new StatusRequirement<>(Set.of(TaskStatus.SUCCESS), RequireType.ALL)));
        map.put(StepStatus.CANCELED,
                Set.of(new StatusRequirement<>(Set.of(TaskStatus.CANCELED), RequireType.MUST),
                    new StatusRequirement<>(
                        Set.of(TaskStatus.FAIL, TaskStatus.CANCELLING,
                            TaskStatus.CREATED, TaskStatus.PAUSED,
                            TaskStatus.PREPARING, TaskStatus.RUNNING), RequireType.HAVE_NO)));
        map.put(StepStatus.CANCELLING,
                Set.of(new StatusRequirement<>(
                        Set.of(TaskStatus.CANCELLING, TaskStatus.CANCELED),
                        RequireType.MUST),
                    new StatusRequirement<>(Set.of(TaskStatus.FAIL), RequireType.HAVE_NO)));
        map.put(StepStatus.PAUSED,
                Set.of(new StatusRequirement<>(Set.of(TaskStatus.PAUSED), RequireType.MUST),
                    new StatusRequirement<>(
                        Set.of(TaskStatus.CANCELLING,
                            TaskStatus.CANCELED,
                            TaskStatus.FAIL),
                        RequireType.HAVE_NO)));
        map.put(StepStatus.READY,
                Set.of(new StatusRequirement<>(Set.of(TaskStatus.READY), RequireType.ALL)));
        map.put(StepStatus.CREATED,
                Set.of(new StatusRequirement<>(Set.of(TaskStatus.CREATED), RequireType.ALL)));
        map.put(StepStatus.FAIL,
                Set.of(new StatusRequirement<>(Set.of(TaskStatus.FAIL), RequireType.ANY)));
        stepStatusRequirementSetMap = Collections.unmodifiableMap(map);
    }

    public static StepStatus desiredStepStatus(Collection<TaskStatus> taskStatuses) {
        if (null == taskStatuses || taskStatuses.isEmpty()) {
            log.info("empty tasks, step status to UNKNOWN");
            return StepStatus.UNKNOWN;
        }
        long startTime = System.currentTimeMillis();
        for (Entry<StepStatus, Set<StatusRequirement<TaskStatus>>> entry : stepStatusRequirementSetMap.entrySet()) {
            log.debug("now checking {}", entry.getKey());
            if (StatusRequirement.match(taskStatuses, entry.getValue())) {
                log.debug("step status {} passed with time {}", entry.getKey(), System.currentTimeMillis() - startTime);
                return entry.getKey();
            }
            log.debug("step status {} not passed", entry.getKey());
        }
        log.warn("step status UNKNOWN returned {}", statusesToString(taskStatuses));
        return StepStatus.UNKNOWN;
    }

    static String statusesToString(Collection<TaskStatus> taskStatuses) {
        StringBuilder sb = new StringBuilder();
        taskStatuses.forEach(ts -> sb.append(" ").append(ts.name()));
        return sb.toString();
    }
}
