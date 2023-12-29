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

package ai.starwhale.mlops.domain.job.bo;

import ai.starwhale.mlops.common.TimeConcern;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.spec.step.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.user.bo.User;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 *
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Job extends TimeConcern {

    Long id;

    String uuid;

    Project project;

    List<StepSpec> stepSpecs;

    Step currentStep;

    /**
     * the SWDSs to run on
     */
    List<DataSet> dataSets;

    /**
     * Model to be run
     */
    Model model;

    /**
     * runtime info of the job
     */
    JobRuntime jobRuntime;

    /**
     * the status of a job
     */
    JobStatus status;

    BizType bizType;
    String bizId;

    JobType type;

    /**
     * job output holding dir
     */
    String outputDir;

    List<Step> steps;

    ResourcePool resourcePool;

    User owner;

    Date createdTime;
    Date finishedTime;
    Long durationMs;
    String comment;

    boolean devMode;
    DevWay devWay;
    String devPassword;
    Date autoReleaseTime;

    Date pinnedTime;

    String virtualJobName;

    public Optional<StepSpec> specOfStep(String stepName) {
        if (CollectionUtils.isEmpty(stepSpecs) || !StringUtils.hasText(stepName)) {
            return Optional.empty();
        }
        return stepSpecs.stream().filter(stepSpec -> stepName.equals(stepSpec.getName())).findFirst();
    }

    public Task getTask(Long taskId) {
        for (Step step : steps) {
            for (Task t : step.getTasks()) {
                if (taskId.equals(t.getId())) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Job job = (Job) o;
        return id.equals(job.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Job{" + "id=" + id + ", uuid='" + uuid + '\'' + '}';
    }
}
