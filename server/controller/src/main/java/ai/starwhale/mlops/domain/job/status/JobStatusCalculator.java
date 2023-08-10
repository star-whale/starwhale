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

package ai.starwhale.mlops.domain.job.status;

import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.domain.job.step.status.StatusRequirement;
import ai.starwhale.mlops.domain.job.step.status.StatusRequirement.RequireType;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JobStatusCalculator {

    final Map<JobStatus, Set<StatusRequirement<StepStatus>>> stepStatusRequirementSetMap;

    public JobStatusCalculator() {
        Map<JobStatus, Set<StatusRequirement<StepStatus>>> map = new LinkedHashMap<>();
        map.put(JobStatus.FAIL, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.FAIL), RequireType.ANY)
        ));
        map.put(JobStatus.UNKNOWN, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.UNKNOWN), RequireType.ANY)
        ));
        map.put(JobStatus.RUNNING, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.RUNNING, StepStatus.READY, StepStatus.CREATED),
                        RequireType.MUST),
                new StatusRequirement<>(
                        Set.of(StepStatus.FAIL, StepStatus.CANCELED, StepStatus.CANCELLING, StepStatus.TO_CANCEL,
                                StepStatus.PAUSED), RequireType.HAVE_NO)
        ));

        map.put(JobStatus.SUCCESS, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.SUCCESS), RequireType.ALL),
                new StatusRequirement<>(Set.of(StepStatus.SUCCESS), RequireType.MUST)
        ));

        map.put(JobStatus.PAUSED, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.PAUSED), RequireType.MUST),
                new StatusRequirement<>(
                        Set.of(StepStatus.FAIL, StepStatus.CANCELED, StepStatus.CANCELLING, StepStatus.TO_CANCEL,
                                StepStatus.RUNNING, StepStatus.READY), RequireType.HAVE_NO)
        ));
        map.put(JobStatus.CANCELLING, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.TO_CANCEL, StepStatus.CANCELLING), RequireType.MUST),
                new StatusRequirement<>(Set.of(StepStatus.FAIL, StepStatus.PAUSED), RequireType.HAVE_NO)
        ));

        map.put(JobStatus.CANCELED, Set.of(
                new StatusRequirement<>(Set.of(StepStatus.CANCELED), RequireType.MUST),
                new StatusRequirement<>(
                        Set.of(StepStatus.FAIL, StepStatus.CANCELLING, StepStatus.RUNNING, StepStatus.PAUSED),
                        RequireType.HAVE_NO)
        ));

        this.stepStatusRequirementSetMap = Collections.unmodifiableMap(map);
        ;
    }

    public JobStatus desiredJobStatus(Collection<StepStatus> stepStatuses) {

        for (Entry<JobStatus, Set<StatusRequirement<StepStatus>>> entry : stepStatusRequirementSetMap.entrySet()) {
            if (StatusRequirement.match(stepStatuses, entry.getValue())) {
                return entry.getKey();
            }
        }
        return JobStatus.UNKNOWN;

    }

}
