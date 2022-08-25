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

package ai.starwhale.mlops.domain.job.step.mapper;

import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface StepMapper {

    void save(@Param("step") StepEntity stepEntity);

    List<StepEntity> findByJobId(@Param("jobId") Long jobId);

    void updateLastStep(@Param("stepId")Long stepId,@Param("lastStepId") Long lastStepId);

    void updateStatus(@Param("stepIds") List<Long> stepIds,@Param("status") StepStatus stepNewStatus);

    void updateFinishedTime(@Param("stepId")Long stepId,@Param("finishedTime")  LocalDateTime finishedTime);

    void updateStartedTime(@Param("stepId")Long stepId,@Param("startedTime")  LocalDateTime startedTime);
}
