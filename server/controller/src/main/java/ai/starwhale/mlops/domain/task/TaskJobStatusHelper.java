/**
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

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.TaskStatusRequirement;
import ai.starwhale.mlops.domain.job.status.TaskStatusRequirement.RequireType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TaskJobStatusHelper {

    public JobStatus desiredJobStatus(Collection<Task> tasks) {
        for (Entry<JobStatus, Set<TaskStatusRequirement>> entry : jobStatusRequirementSetMap.entrySet()) {
            log.debug("now checking {}",entry.getKey());
            if (match(tasks, entry.getValue())) {
                log.debug("job status {} passed",entry.getKey());
                return entry.getKey();
            }
            log.debug("job status {} not passed",entry.getKey());
        }
        log.debug("job status UNKNOWN returned ");
        return JobStatus.UNKNOWN;
    }

    /**
     * set of TRs are and relationship
     */
    Map<JobStatus, Set<TaskStatusRequirement>> jobStatusRequirementSetMap = Map.ofEntries(
        new SimpleEntry<>(JobStatus.RUNNING, Set.of(new TaskStatusRequirement(
                Set.of(TaskStatus.CREATED,TaskStatus.ASSIGNING, TaskStatus.PREPARING, TaskStatus.RUNNING), TaskType.PPL,
                RequireType.MUST)
            , new TaskStatusRequirement(
                Set.of(TaskStatus.PAUSED, TaskStatus.TO_CANCEL, TaskStatus.CANCELLING,
                    TaskStatus.CANCELED, TaskStatus.FAIL), TaskType.PPL, RequireType.HAVE_NO)
            , new TaskStatusRequirement(Set.of(TaskStatus.values()), TaskType.CMP,
                RequireType.HAVE_NO)))
        , new SimpleEntry<>(JobStatus.TO_COLLECT_RESULT, Set.of(
            new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.PPL, RequireType.ALL)
            , new TaskStatusRequirement(Set.of(TaskStatus.values()), TaskType.CMP,
                RequireType.HAVE_NO)))
        , new SimpleEntry<>(JobStatus.COLLECTING_RESULT, Set.of(
            new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.PPL, RequireType.ALL)
            , new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.CMP, RequireType.HAVE_NO)
            , new TaskStatusRequirement(
                Set.of(TaskStatus.FAIL, TaskStatus.TO_CANCEL, TaskStatus.CANCELLING,
                    TaskStatus.CANCELED), null, RequireType.HAVE_NO)
            , new TaskStatusRequirement(
                Set.of(TaskStatus.CREATED,TaskStatus.ASSIGNING, TaskStatus.PREPARING, TaskStatus.RUNNING),
                TaskType.CMP, RequireType.MUST)))
        , new SimpleEntry<>(JobStatus.SUCCESS,
            Set.of(new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.PPL, RequireType.ALL)
                , new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.CMP, RequireType.ALL)
                , new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.CMP, RequireType.MUST)))
        , new SimpleEntry<>(JobStatus.CANCELING,
            Set.of(new TaskStatusRequirement(Set.of(TaskStatus.CANCELLING,TaskStatus.TO_CANCEL), null, RequireType.MUST)
                , new TaskStatusRequirement(Set.of(TaskStatus.FAIL), null, RequireType.HAVE_NO)))
        , new SimpleEntry<>(JobStatus.CANCELED,
            Set.of(new TaskStatusRequirement(Set.of(TaskStatus.CANCELED), null, RequireType.MUST)
                , new TaskStatusRequirement(
                    Set.of(TaskStatus.FAIL, TaskStatus.CANCELLING, TaskStatus.TO_CANCEL,
                        TaskStatus.CREATED, TaskStatus.ASSIGNING, TaskStatus.PAUSED,
                        TaskStatus.PREPARING, TaskStatus.RUNNING), null, RequireType.HAVE_NO)))
        , new SimpleEntry<>(JobStatus.FAIL,
            Set.of(new TaskStatusRequirement(Set.of(TaskStatus.FAIL), null, RequireType.ANY)))
        , new SimpleEntry<>(JobStatus.PAUSED,
            Set.of(new TaskStatusRequirement(Set.of(TaskStatus.CREATED), null, RequireType.HAVE_NO)
                , new TaskStatusRequirement(Set.of(TaskStatus.PAUSED), null, RequireType.MUST)
                , new TaskStatusRequirement(
                    Set.of(TaskStatus.TO_CANCEL, TaskStatus.CANCELLING, TaskStatus.CANCELED,
                        TaskStatus.FAIL), null, RequireType.HAVE_NO)
                , new TaskStatusRequirement(Set.of(TaskStatus.values()), TaskType.CMP,
                    RequireType.HAVE_NO)))
    );

    boolean match(Collection<Task> tasks, Set<TaskStatusRequirement> requirements) {
        Map<Boolean, List<TaskStatusRequirement>> requireTypeListMap = requirements.stream()
            .collect(Collectors.groupingBy(tr -> tr.getRequireType() == RequireType.ANY));

        List<TaskStatusRequirement> anyR = requireTypeListMap.get(true);
        if (null != anyR) {
            for (TaskStatusRequirement tr : anyR) {
                if (tr.fit(tasks)) {
                    return true;
                }
            }
            return false;
        }

        List<TaskStatusRequirement> negativeR = requireTypeListMap.get(false);
        if (null != negativeR) {
            for (TaskStatusRequirement tr : negativeR) {
                if (!tr.fit(tasks)) {
                    return false;
                }
            }
        }

        return true;
    }

}
