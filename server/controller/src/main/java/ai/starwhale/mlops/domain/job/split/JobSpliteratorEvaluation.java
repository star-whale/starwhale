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

package ai.starwhale.mlops.domain.job.split;

import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * split job by swds index
 */
@Slf4j
@Service
public class JobSpliteratorEvaluation implements JobSpliterator {

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_MYSQL_INSERTION_SIZE = 500;
    private final StoragePathCoordinator storagePathCoordinator;
    private final TaskMapper taskMapper;
    private final JobMapper jobMapper;
    private final StepMapper stepMapper;
    private final JobSpecParser jobSpecParser;
    /**
     * when task amount exceeds 1000, bach insertion will emit an error
     */
    @Value("${sw.task.size}")
    Integer amountOfTasks = 256;

    public JobSpliteratorEvaluation(StoragePathCoordinator storagePathCoordinator,
            TaskMapper taskMapper,
            JobMapper jobMapper,
            StepMapper stepMapper, JobSpecParser jobSpecParser) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.stepMapper = stepMapper;
        this.jobSpecParser = jobSpecParser;
    }

    /**
     * split job into two steps transactional jobStatus->READY firstStepTaskStatus->READY
     * followerStepTaskStatus->CREATED
     */
    @Override
    @Transactional
    public List<StepEntity> split(JobEntity job) {
        if (JobStatus.CREATED != job.getJobStatus()) {
            throw new SwValidationException(ValidSubject.JOB).tip("job has been split already!");
        }
        List<StepSpec> stepSpecs;
        try {
            if (!StringUtils.hasText(job.getStepSpec())) {
                stepSpecs = jobSpecParser.parseStepFromYaml(job.getModelVersion().getEvalJobs());
            } else {
                stepSpecs = jobSpecParser.parseStepFromYaml(job.getStepSpec());
            }
        } catch (JsonProcessingException e) {
            log.error("parsing step specification error", e);
            throw new SwValidationException(ValidSubject.MODEL);
        }

        List<StepEntity> stepEntities = new ArrayList<>();
        Map<String, List<String>> allDependencies = new HashMap<>();
        Map<String, Tuple2<StepEntity, StepSpec>> nameMapping = new HashMap<>();

        for (StepSpec stepSpec : stepSpecs) {
            boolean firstStep = CollectionUtils.isEmpty(stepSpec.getNeeds());

            StepEntity stepEntity = StepEntity.builder()
                    .uuid(UUID.randomUUID().toString())
                    .jobId(job.getId())
                    .name(stepSpec.getStepName())
                    .taskNum(stepSpec.getTaskNum())
                    .concurrency(stepSpec.getConcurrency())
                    .status(firstStep ? StepStatus.READY : StepStatus.CREATED)
                    .build();
            stepMapper.save(stepEntity);
            stepEntities.add(stepEntity);
            allDependencies.put(stepSpec.getStepName(), stepSpec.getNeeds() == null ? List.of() : stepSpec.getNeeds());
            nameMapping.put(stepSpec.getStepName(), new Tuple2<>(stepEntity, stepSpec));
        }

        for (StepEntity stepEntity : stepEntities) {
            List<String> dependencies = allDependencies.get(stepEntity.getName());
            for (String dependency : dependencies) {
                // the current implementation is serial, so dependency only one
                stepEntity.setLastStepId(nameMapping.get(dependency)._1().getId());
            }
            // TODO: replace this implement with only send ds uri and task index to container
            List<TaskEntity> taskEntities = new LinkedList<>();
            for (int i = 0; i < stepEntity.getTaskNum(); i++) {
                final String taskUuid = UUID.randomUUID().toString();
                taskEntities.add(TaskEntity.builder()
                        .stepId(stepEntity.getId())
                        .outputPath(
                                storagePathCoordinator.allocateTaskResultPath(job.getJobUuid(), taskUuid))
                        .taskRequest(JSONUtil.toJsonStr(
                                        TaskRequest.builder()
                                                .total(stepEntity.getTaskNum())
                                                .index(i)
                                                .runtimeResources(
                                                        nameMapping.get(stepEntity.getName())._2.getResources())
                                                .build()
                                )
                        )
                        .taskStatus(TaskStatus.valueOf(stepEntity.getStatus().name()))
                        .taskUuid(taskUuid)
                        .build());
            }

            // update step's lastStepId and save tasks
            stepMapper.updateLastStep(stepEntity.getId(), stepEntity.getLastStepId());
            BatchOperateHelper.doBatch(taskEntities,
                    ts -> taskMapper.addAll(ts.parallelStream().collect(Collectors.toList())),
                    MAX_MYSQL_INSERTION_SIZE);
        }
        // update job status
        jobMapper.updateJobStatus(List.of(job.getId()), JobStatus.READY);
        return stepEntities;
    }
}
