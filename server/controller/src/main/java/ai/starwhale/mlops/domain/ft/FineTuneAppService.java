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

package ai.starwhale.mlops.domain.ft;

import ai.starwhale.mlops.api.protocol.ft.FineTuneCreateRequest;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneMapper;
import ai.starwhale.mlops.domain.ft.po.FineTuneEntity;
import ai.starwhale.mlops.domain.ft.vo.FineTuneVo;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class FineTuneAppService {

    final JobCreator jobCreator;

    final FineTuneMapper fineTuneMapper;

    final JobMapper jobMapper;

    private final JobSpecParser jobSpecParser;

    final ModelDao modelDao;

    private final DatasetDao datasetDao;

    final String instanceUri;

    public FineTuneAppService(
            JobCreator jobCreator, FineTuneMapper fineTuneMapper, JobMapper jobMapper, JobSpecParser jobSpecParser,
            ModelDao modelDao,
            @Value("${sw.instance-uri}") String instanceUri,
            DatasetDao datasetDao
    ) {
        this.jobCreator = jobCreator;
        this.fineTuneMapper = fineTuneMapper;
        this.jobMapper = jobMapper;
        this.jobSpecParser = jobSpecParser;
        this.modelDao = modelDao;
        this.datasetDao = datasetDao;
        this.instanceUri = instanceUri;
    }


    public void createFineTune(
            Long spaceId,
            Project project,
            FineTuneCreateRequest request,
            User creator
    ) {
        FineTuneEntity ft = FineTuneEntity.builder()
                .jobId(-1L)
                .spaceId(spaceId)
                .evalDatasets(request.getEvalDatasetVersionIds())
                .trainDatasets(request.getDatasetVersionIds())
                .baseModelVersionId(request.getModelVersionId())
                .build();
        fineTuneMapper.add(ft);
        request = addEnvToRequest(ft.getId(), request);
        Job job = jobCreator.createJob(
                UserJobCreateRequest.builder()
                        .modelVersionId(request.getModelVersionId())
                        .runtimeVersionId(request.getRuntimeVersionId())
                        .datasetVersionIds(request.getDatasetVersionIds())
                        .devMode(request.isDevMode())
                        .devPassword(request.getDevPassword())
                        .ttlInSec(request.getTimeToLiveInSec())
                        .project(project)
                        .user(creator)
                        .comment(request.getComment())
                        .resourcePool(request.getResourcePool())
                        .handler(request.getHandler())
                        .stepSpecOverWrites(request.getStepSpecOverWrites())
                        .jobType(JobType.FINE_TUNE)
                        .build()
        );
        fineTuneMapper.updateJobId(ft.getId(), job.getId());
    }

    private FineTuneCreateRequest addEnvToRequest(Long id, FineTuneCreateRequest request) {
        var stepSpecOverWrites = request.getStepSpecOverWrites();
        var handler = request.getHandler();
        if ((!StringUtils.hasText(stepSpecOverWrites) && !StringUtils.hasText(handler))
                || (StringUtils.hasText(stepSpecOverWrites) && StringUtils.hasText(handler))) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "handler or stepSpec must be provided only one"),
                    HttpStatus.BAD_REQUEST
            );
        }
        List<StepSpec> steps;
        try {
            steps = StringUtils.hasText(stepSpecOverWrites)
                    ? jobSpecParser.parseAndFlattenStepFromYaml(stepSpecOverWrites)
                    : jobSpecParser.parseStepFromYaml(stepSpecOfModelVersion(request.getModelVersionId()), handler);
            for (var s : steps) {
                List<Env> env = s.getEnv();
                if (null == env) {
                    env = new ArrayList<>();
                }
                if (!CollectionUtils.isEmpty(request.getEvalDatasetVersionIds())) {
                    String evalDataSetUris = request.getEvalDatasetVersionIds().stream().map(dsv -> {
                        DatasetVersion datasetVersion = datasetDao.getDatasetVersion(dsv);
                        return String.format(
                                ContainerSpecification.FORMATTER_URI_ARTIFACT,
                                instanceUri,
                                datasetVersion.getProjectId(),
                                "dataset",
                                datasetVersion.getDatasetName(),
                                datasetVersion.getVersionName()
                        );

                    }).collect(Collectors.joining(" "));
                    env.add(new Env("SW_FINETUNE_VALIDATION_DATASET_URI", evalDataSetUris));
                }
                env.add(new Env("SW_SERVER_TRIGGERED_FINETUNE_ID", id.toString()));
                s.setEnv(env);
                s.verifyStepSpecArgs();
            }
            stepSpecOverWrites = Constants.yamlMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "failed to parse job step", e), HttpStatus.BAD_REQUEST);
        }
        request.setStepSpecOverWrites(stepSpecOverWrites);
        return request;
    }

    private String stepSpecOfModelVersion(Long modelVersionId) {
        return ModelVersion.fromEntity(modelDao.findVersionById(modelVersionId)).getJobs();
    }


    public PageInfo<FineTuneVo> list(Long spaceId, Integer pageNum, Integer pageSize) {
        try (var ph = PageHelper.startPage(pageNum, pageSize)) {
            return PageInfo.of(fineTuneMapper.list(spaceId).stream().map(fineTuneEntity -> {
                Long jobId = fineTuneEntity.getJobId();
                JobEntity job = jobMapper.findJobById(jobId);
                fineTuneEntity.getEvalDatasets();
                fineTuneEntity.getTrainDatasets();
                fineTuneEntity.getBaseModelVersionId();
                fineTuneEntity.getTargetModelVersionId();
                return FineTuneVo.builder()
                        .id(fineTuneEntity.getId())
                        .jobId(jobId)
                        .status(job.getJobStatus())
                        .startTime(job.getCreatedTime().getTime())
                        .endTime(job.getFinishedTime().getTime())
                        .evalDatasets(List.of())//TODO
                        .trainDatasets(List.of())//TODO
                        .baseModel(null)//TODO
                        .targetModel(null)//TODO
                        .build();
            }).collect(Collectors.toList()));
        }
    }

    public void evalFt(List<Long> evalDatasetIds, Long runtimeId, String handerSpec, Map<String, String> envs) {

    }

    public void releaseFt(Long ftId) {

    }

    public void attachTargetModel(Long id, ModelVersionEntity modelVersionEntity) {
        fineTuneMapper.updateTargetModel(id, modelVersionEntity.getId());
    }
}
