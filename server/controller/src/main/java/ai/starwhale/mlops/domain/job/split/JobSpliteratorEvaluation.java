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

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.api.protocol.report.resp.SWDSBlockVO;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.index.SWDSBlockSerializer;
import ai.starwhale.mlops.domain.swds.index.SWDSIndex;
import ai.starwhale.mlops.domain.swds.index.SWDSIndexLoader;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.ResultPathConverter;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.WatchableTask;
import ai.starwhale.mlops.domain.task.status.WatchableTaskFactory;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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

    private final ResultPathConverter resultPathConverter;

    private final StepMapper stepMapper;

    private final StepConverter stepConverter;

    private final WatchableTaskFactory watchableTaskFactory;

    public JobSpliteratorEvaluation(StoragePathCoordinator storagePathCoordinator,
        SWDSIndexLoader swdsIndexLoader, SWDSBlockSerializer swdsBlockSerializer,
        TaskMapper taskMapper, JobMapper jobMapper, TaskBoConverter taskBoConverter,
        ResultPathConverter resultPathConverter,
        StepMapper stepMapper, StepConverter stepConverter,
        WatchableTaskFactory watchableTaskFactory) {
        this.storagePathCoordinator = storagePathCoordinator;
        this.swdsIndexLoader = swdsIndexLoader;
        this.swdsBlockSerializer = swdsBlockSerializer;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskBoConverter = taskBoConverter;
        this.resultPathConverter = resultPathConverter;
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.watchableTaskFactory = watchableTaskFactory;
    }

    /**
     * when task amount exceeds 1000, bach insertion will emit an error
     */
    @Value("${sw.taskSize}")
    Integer amountOfTasks =256;

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    final static Integer MAX_MYSQL_INSERTION_SIZE=500;

    public static final String[] STEP_NAMES = new String[]{"PPL","CMP"};
    /**
     * split job into two steps 1. ppl 2. cmp
     * get all data blocks and split them by a simple random number
     * transactional jobStatus->READY pplTaskStatus->READY cmpTaskStatus->CREATED
     */
    @Override
    @Transactional
    public List<Step> split(Job job) {
        StepEntity stepEntityPPL = StepEntity.builder().jobId(job.getId()).name(STEP_NAMES[0]).status(
            StepStatus.READY).uuid(UUID.randomUUID().toString()).build();
        stepMapper.save(stepEntityPPL);
        Step stepPPL = stepConverter.fromEntity(stepEntityPPL);
        stepPPL.setJob(job);
        final List<SWDataSet> swDataSets = job.getSwDataSets();
        final Map<Integer,List<SWDSBlockVO>> swdsBlocks = swDataSets.parallelStream()
            .map(this::extractSWDS)
            .flatMap(Collection::stream)
            .collect(Collectors.groupingBy(blk-> ThreadLocalRandom.current().nextInt(amountOfTasks)));//one block on task
        List<TaskEntity> pplTaskEntities;
        try {
            pplTaskEntities = buildPPLTaskEntities(job, stepEntityPPL,swdsBlocks);
        } catch (JsonProcessingException e) {
            log.error("error swds index  ",e);
            throw new SWValidationException(ValidSubject.SWDS);
        }
        BatchOperateHelper.doBatch(pplTaskEntities,ts->taskMapper.addAll(ts.parallelStream().collect(Collectors.toList())),MAX_MYSQL_INSERTION_SIZE);
        jobMapper.updateJobStatus(List.of(job.getId()), JobStatus.READY);

        List<Task> pplTasks = taskBoConverter.fromTaskEntity(pplTaskEntities, stepPPL);
        stepPPL.setTasks(pplTasks.parallelStream().map(t->watchableTaskFactory.wrapTask(t)).collect(
            Collectors.toList()));

        StepEntity stepEntityCMP = StepEntity.builder().jobId(job.getId()).name(STEP_NAMES[1]).status(
            StepStatus.CREATED).lastStepId(stepEntityPPL.getId()).uuid(UUID.randomUUID().toString()).build();
        stepMapper.save(stepEntityCMP);
        Step stepCMP = stepConverter.fromEntity(stepEntityCMP);
        stepCMP.setJob(job);
        TaskEntity cmpTaskEntity = TaskEntity.builder()
            .stepId(stepEntityCMP.getId())
            .taskType(TaskType.CMP)
            .resultPath(job.getResultDir())
            .taskStatus(TaskStatus.CREATED)
            .taskUuid(UUID.randomUUID().toString())
            .build();
        taskMapper.addTask(cmpTaskEntity);
        Task cmpTask = taskBoConverter.transformTask(stepCMP,cmpTaskEntity);
        stepCMP.setTasks(List.of(watchableTaskFactory.wrapTask(cmpTask)));

        stepPPL.setNextStep(stepCMP);
        job.setSteps(List.of(stepPPL,stepCMP));
        job.setCurrentStep(stepPPL);
        return job.getSteps();
    }

    private List<SWDSBlockVO>  extractSWDS(SWDataSet swDataSet){
        SWDSIndex swdsIndex = swdsIndexLoader.load(swDataSet.getIndexPath());
        return swdsIndex.getSwdsBlockList().parallelStream().map(swdsBlock -> {
            SWDSBlockVO swdsBlockVO = new SWDSBlockVO();
            BeanUtils.copyProperties(swdsBlock,swdsBlockVO);
            swdsBlockVO.prependDSPath(swDataSet.getPath());
            swdsBlockVO.setDsName(swDataSet.getName());
            swdsBlockVO.setDsVersion(swDataSet.getVersion());
            return swdsBlockVO;
        }).collect(Collectors.toList());
    }

    private List<TaskEntity> buildPPLTaskEntities(Job job, StepEntity stepEntityPPL,Map<Integer, List<SWDSBlockVO>> swdsBlocks)
        throws JsonProcessingException {
        List<TaskEntity> taskEntities = new LinkedList<>();
        for(Entry<Integer, List<SWDSBlockVO>> entry:swdsBlocks.entrySet()) {
            final String taskUuid = UUID.randomUUID().toString();
            taskEntities.add(TaskEntity.builder()
                .stepId(stepEntityPPL.getId())
                .resultPath(resultPathConverter.toString(new ResultPath(storagePath(job.getUuid(), taskUuid))))
                .taskRequest(swdsBlockSerializer.toString(entry.getValue()))
                .taskStatus(TaskStatus.READY)
                .taskUuid(taskUuid)
                .taskType(TaskType.PPL)
                .build());
        }
        return taskEntities;
    }

    private String storagePath(String jobId,String taskId) {
        return storagePathCoordinator.generateTaskResultPath(jobId,taskId);
    }
}
