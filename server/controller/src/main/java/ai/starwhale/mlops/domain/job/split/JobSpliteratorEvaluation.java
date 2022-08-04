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

import ai.starwhale.mlops.api.protocol.report.resp.SWDSBlockVO;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.parser.JobParser;
import ai.starwhale.mlops.domain.job.parser.StepMetaData;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
import ai.starwhale.mlops.domain.swds.index.SWDSBlockSerializer;
import ai.starwhale.mlops.domain.swds.bo.SWDSIndex;
import ai.starwhale.mlops.domain.swds.index.SWDSIndexLoader;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.WatchableTaskFactory;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * split job by swds index
 */
@Slf4j
@Service
public class JobSpliteratorEvaluation implements JobSpliterator {

    private final StoragePathCoordinator storagePathCoordinator;

    private final SWDSIndexLoader swdsIndexLoader;

    private final SWDSBlockSerializer swdsBlockSerializer;

    private final TaskMapper taskMapper;

    private final JobMapper jobMapper;

    private final TaskBoConverter taskBoConverter;

    private final StepMapper stepMapper;

    private final StepConverter stepConverter;

    private final WatchableTaskFactory watchableTaskFactory;

    public JobSpliteratorEvaluation(StoragePathCoordinator storagePathCoordinator,
                                    SWDSIndexLoader swdsIndexLoader, SWDSBlockSerializer swdsBlockSerializer,
                                    TaskMapper taskMapper, JobMapper jobMapper, TaskBoConverter taskBoConverter,
                                    StepMapper stepMapper, StepConverter stepConverter,
                                    WatchableTaskFactory watchableTaskFactory) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.swdsIndexLoader = swdsIndexLoader;
        this.swdsBlockSerializer = swdsBlockSerializer;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskBoConverter = taskBoConverter;
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.watchableTaskFactory = watchableTaskFactory;
    }

    /**
     * when task amount exceeds 1000, bach insertion will emit an error
     */
    @Value("${sw.taskSize}")
    Integer amountOfTasks = 256;

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    final static Integer MAX_MYSQL_INSERTION_SIZE = 500;

    public static final String[] STEP_NAMES = new String[]{"PPL", "CMP"};

    /**
     * split job into two steps 1. ppl 2. cmp
     * get all data blocks and split them by a simple random number
     * transactional jobStatus->READY pplTaskStatus->READY cmpTaskStatus->CREATED
     */
    @Override
    @Transactional
    public List<StepEntity> split(Job job) {
        // read swmp yaml
        List<StepMetaData> stepMetaDatas = JobParser.parseStepFromYaml(job.getEvalJobContent());
        List<StepEntity> stepEntities = new ArrayList<>();
        Map<String, List<String>> allDependencies = new HashMap<>();
        Map<String, StepEntity> nameMapping = new HashMap<>();

        for (StepMetaData stepMetaData : stepMetaDatas) {
            boolean isReady = CollectionUtils.isEmpty(stepMetaData.getDependency());

            StepEntity stepEntity = StepEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .jobId(job.getId())
                .name(stepMetaData.getStepName())
                .taskNum(stepMetaData.getTaskNum())
                .concurrency(stepMetaData.getConcurrency())
                .status(isReady ? StepStatus.READY : StepStatus.CREATED)
                .build();
            stepEntities.add(stepEntity);
            allDependencies.put(stepMetaData.getStepName(), stepMetaData.getDependency());
            nameMapping.put(stepMetaData.getStepName(), stepEntity);
        }

        for (StepEntity stepEntity : stepEntities) {
            List<String> dependencies = allDependencies.get(stepEntity.getName());
            for (String dependency : dependencies) {
                // the current implementation is serial, so dependency only one
                stepEntity.setLastStepId(nameMapping.get(dependency).getId());
            }
            // TODO: replace this implement with only send ds uri and task index to container
            final List<SWDataSet> swDataSets = job.getSwDataSets();
            final Map<Integer, List<SWDSBlockVO>> swdsBlocks = swDataSets.parallelStream()
                .map(this::extractSWDS)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(blk -> ThreadLocalRandom.current().nextInt(stepEntity.getTaskNum())));//one block on task
            List<TaskEntity> taskEntities = new LinkedList<>();
            try {
                var index = 0;
                for (Entry<Integer, List<SWDSBlockVO>> entry : swdsBlocks.entrySet()) {
                    final String taskUuid = UUID.randomUUID().toString();
                    taskEntities.add(TaskEntity.builder()
                        .stepId(stepEntity.getId())
                        .taskIndex(index)
                        .resultPath(storagePath(job.getUuid(), taskUuid))
                        .taskRequest(swdsBlockSerializer.toString(entry.getValue()))
                        .taskStatus(TaskStatus.valueOf(stepEntity.getStatus().name()))
                        .taskUuid(taskUuid)
                        //.taskType(TaskType.PPL)
                        .build());
                    index++;
                }
            } catch (JsonProcessingException e) {
                log.error("error swds index  ", e);
                throw new SWValidationException(ValidSubject.SWDS);
            }
            // save step and tasks
            stepMapper.save(stepEntity);
            BatchOperateHelper.doBatch(taskEntities, ts -> taskMapper.addAll(ts.parallelStream().collect(Collectors.toList())), MAX_MYSQL_INSERTION_SIZE);
        }
        // update job status
        jobMapper.updateJobStatus(List.of(job.getId()), JobStatus.READY);
        return stepEntities;
    }

    private List<SWDSBlockVO> extractSWDS(SWDataSet swDataSet) {
        SWDSIndex swdsIndex = swdsIndexLoader.load(swDataSet.getIndexPath());
        return swdsIndex.getSwdsBlockList().parallelStream().map(swdsBlock -> {
            SWDSBlockVO swdsBlockVO = new SWDSBlockVO();
            BeanUtils.copyProperties(swdsBlock, swdsBlockVO);
            swdsBlockVO.prependDSPath(swDataSet.getPath());
            swdsBlockVO.setDsName(swDataSet.getName());
            swdsBlockVO.setDsVersion(swDataSet.getVersion());
            return swdsBlockVO;
        }).collect(Collectors.toList());
    }

    private List<TaskEntity> buildTaskEntities(Job job, StepEntity stepEntityPPL, Map<Integer, List<SWDSBlockVO>> swdsBlocks)
        throws JsonProcessingException {
        List<TaskEntity> taskEntities = new LinkedList<>();
        for (Entry<Integer, List<SWDSBlockVO>> entry : swdsBlocks.entrySet()) {
            final String taskUuid = UUID.randomUUID().toString();
            taskEntities.add(TaskEntity.builder()
                .stepId(stepEntityPPL.getId())
                .resultPath(storagePath(job.getUuid(), taskUuid))
                .taskRequest(swdsBlockSerializer.toString(entry.getValue()))
                .taskStatus(TaskStatus.READY)
                .taskUuid(taskUuid)
                //.taskType(TaskType.PPL)
                .build());
        }
        return taskEntities;
    }

    private String storagePath(String jobId, String taskId) {
        return storagePathCoordinator.generateTaskResultPath(jobId, taskId);
    }
}
