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
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * split job by swds index
 */
@Slf4j
@Service
public class JobSpliteratorImpl implements JobSpliterator {

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_MYSQL_INSERTION_SIZE = 500;
    private final StoragePathCoordinator storagePathCoordinator;
    private final TaskMapper taskMapper;
    private final JobDao jobDao;
    private final StepMapper stepMapper;
    private final JobSpecParser jobSpecParser;

    public JobSpliteratorImpl(StoragePathCoordinator storagePathCoordinator,
                              TaskMapper taskMapper,
                              JobDao jobDao,
                              StepMapper stepMapper, JobSpecParser jobSpecParser) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.taskMapper = taskMapper;
        this.jobDao = jobDao;
        this.stepMapper = stepMapper;
        this.jobSpecParser = jobSpecParser;
    }

    /**
     * split job into two steps transactional jobStatus->READY
     * firstStepTaskStatus->READY
     * followerStepTaskStatus->CREATED
     */
    @Override
    @Transactional
    public List<StepEntity> split(Job job) {
        if (JobStatus.CREATED != job.getStatus()) {
            throw new SwValidationException(ValidSubject.JOB, "job has been split already!");
        }
        List<StepSpec> stepSpecs;
        try {
            if (!StringUtils.hasText(job.getStepSpec())) {
                log.error("job:{} don't have step specification", job.getId());
                throw new SwValidationException(ValidSubject.JOB);
            } else {
                stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(job.getStepSpec());
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
                    .name(stepSpec.getName())
                    .taskNum(stepSpec.getReplicas())
                    .concurrency(stepSpec.getConcurrency())
                    .status(firstStep ? StepStatus.READY : StepStatus.CREATED)
                    .build();
            stepMapper.save(stepEntity);
            stepEntities.add(stepEntity);
            allDependencies.put(stepSpec.getName(), stepSpec.getNeeds() == null ? List.of() : stepSpec.getNeeds());
            nameMapping.put(stepSpec.getName(), new Tuple2<>(stepEntity, stepSpec));
        }

        for (StepEntity stepEntity : stepEntities) {
            List<String> dependencies = allDependencies.get(stepEntity.getName());
            for (String dependency : dependencies) {
                // the current implementation is serial, so dependency only one
                stepEntity.setLastStepId(nameMapping.get(dependency)._1().getId());
            }
            // TODO: replace this implement with only send ds uri and task index to
            // container
            List<TaskEntity> taskEntities = new LinkedList<>();
            for (int i = 0; i < stepEntity.getTaskNum(); i++) {
                final String taskUuid = UUID.randomUUID().toString();
                taskEntities.add(TaskEntity.builder()
                        .stepId(stepEntity.getId())
                        .outputPath(
                                storagePathCoordinator.allocateTaskResultPath(job.getUuid(), taskUuid))
                        .taskRequest(JSONUtil.toJsonStr(
                                TaskRequest.builder()
                                        .total(stepEntity.getTaskNum())
                                        .index(i)
                                        .env(nameMapping.get(stepEntity.getName())._2().getEnv())
                                        .jobName(nameMapping.get(stepEntity.getName())._2().getJobName())
                                        .runtimeResources(
                                                nameMapping.get(stepEntity.getName())._2().getResources())
                                        .build()))
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
        job.setStatus(JobStatus.READY);
        jobDao.updateJobStatus(job.getId(), JobStatus.READY);
        return stepEntities;
    }
}
