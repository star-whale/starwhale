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

import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.event.EventService;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneMapper;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneSpaceMapper;
import ai.starwhale.mlops.domain.ft.po.FineTuneEntity;
import ai.starwhale.mlops.domain.ft.po.FineTuneSpaceEntity;
import ai.starwhale.mlops.domain.ft.vo.FineTuneVo;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.converter.UserJobConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class FineTuneAppService {

    static final String EVALUATION_SUMMARY_TABLE_FORMAT = "ftspace/%d/eval/summary";

    private final FeaturesProperties featuresProperties;

    final JobCreator jobCreator;

    final FineTuneMapper fineTuneMapper;

    final JobMapper jobMapper;

    private final JobSpecParser jobSpecParser;

    private final IdConverter idConverter;

    final ModelDao modelDao;

    private final DatasetDao datasetDao;

    private final EventService eventService;

    final String instanceUri;

    final FineTuneSpaceMapper fineTuneSpaceMapper;

    final UserJobConverter userJobConverter;

    final JobConverter jobConverter;

    final ModelService modelService;

    final DatasetService datasetService;

    public FineTuneAppService(
            FeaturesProperties featuresProperties,
            JobCreator jobCreator,
            FineTuneMapper fineTuneMapper,
            JobMapper jobMapper,
            JobSpecParser jobSpecParser,
            IdConverter idConverter,
            ModelDao modelDao,
            @Value("${sw.instance-uri}") String instanceUri,
            DatasetDao datasetDao,
            FineTuneSpaceMapper fineTuneSpaceMapper,
            UserJobConverter userJobConverter,
            EventService eventService,
            JobConverter jobConverter,
            ModelService modelService,
            DatasetService datasetService
    ) {
        this.featuresProperties = featuresProperties;
        this.jobCreator = jobCreator;
        this.fineTuneMapper = fineTuneMapper;
        this.jobMapper = jobMapper;
        this.jobSpecParser = jobSpecParser;
        this.idConverter = idConverter;
        this.modelDao = modelDao;
        this.datasetDao = datasetDao;
        this.instanceUri = instanceUri;
        this.fineTuneSpaceMapper = fineTuneSpaceMapper;
        this.userJobConverter = userJobConverter;
        this.eventService = eventService;
        this.jobConverter = jobConverter;
        this.modelService = modelService;
        this.datasetService = datasetService;
    }


    @Transactional
    public Long createFineTune(
            String projectId,
            Long spaceId,
            JobRequest request
    ) {
        this.checkFeatureEnabled();
        FineTuneEntity ft = FineTuneEntity.builder()
                .jobId(-1L)
                .spaceId(spaceId)
                .evalDatasets(idConverter.revertList(request.getEvalDatasetVersionIds()))
                .trainDatasets(idConverter.revertList(request.getDatasetVersionIds()))
                .baseModelVersionId(idConverter.revert(request.getModelVersionId()))
                .build();
        fineTuneMapper.add(ft);
        // add ft env to spec
        var datasets = request.getEvalDatasetVersionIds();
        request.setStepSpecOverWrites(rewriteSpecEnvForRequest(
                idConverter.revert(request.getModelVersionId()),
                request.getHandler(),
                request.getStepSpecOverWrites(),
                () -> {
                    List<Env> env = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(datasets)) {
                        String evalDataSetUris = datasets.stream().map(dsv -> {
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
                    env.add(new Env("SW_SERVER_TRIGGERED_FINETUNE_ID", ft.getId().toString()));
                    List<ModelVo> modelVersions =
                            modelService.findModelByVersionId(List.of(idConverter.revert(request.getModelVersionId())));
                    if(CollectionUtils.isEmpty(modelVersions)) {
                        throw new StarwhaleApiException(
                                new SwValidationException(ValidSubject.JOB, "no model found for the requested version"),
                                HttpStatus.BAD_REQUEST
                        );
                    }
                    ModelVo modelVo = modelVersions.get(0);
                    env.add(new Env("SW_FINETUNE_TARGET_MODEL", modelVo.getName()));
                    return env;
                }
        ));
        request.setType(JobType.FINE_TUNE);
        Job job = jobCreator.createJob(userJobConverter.convert(projectId, request));
        fineTuneMapper.updateJobId(ft.getId(), job.getId());
        return job.getId();
    }

    public Long createEvaluationJob(String projectId, Long spaceId, JobRequest request) {
        // add ft eval's env to spec
        request.setStepSpecOverWrites(rewriteSpecEnvForRequest(
                idConverter.revert(request.getModelVersionId()),
                request.getHandler(),
                request.getStepSpecOverWrites(),
                () -> List.of(new Env("SW_EVALUATION_SUMMARY_TABLE",
                        String.format(EVALUATION_SUMMARY_TABLE_FORMAT, spaceId)))
        ));
        request.setType(JobType.EVALUATION);
        var job = jobCreator.createJob(userJobConverter.convert(projectId, request));
        eventService.addInternalJobInfoEvent(job.getId(), "Evaluation Job created");
        return job.getId();
    }

    private String rewriteSpecEnvForRequest(
            Long modelVersionId, String handler, String stepSpecOverWrites, Supplier<List<Env>> envSupplier) {
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
                    : jobSpecParser.parseStepFromYaml(stepSpecOfModelVersion(modelVersionId), handler);
            for (var s : steps) {
                List<Env> env = s.getEnv();
                if (null == env) {
                    env = new ArrayList<>();
                }
                env.addAll(envSupplier.get());
                s.setEnv(env);
                s.verifyStepSpecArgs();
            }
            stepSpecOverWrites = Constants.yamlMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "failed to parse job step", e), HttpStatus.BAD_REQUEST);
        }
        return stepSpecOverWrites;
    }

    private String stepSpecOfModelVersion(Long modelVersionId) {
        return ModelVersion.fromEntity(modelDao.findVersionById(modelVersionId)).getJobs();
    }

    public PageInfo<FineTuneVo> list(Long spaceId, Integer pageNum, Integer pageSize) {
        this.checkFeatureEnabled();
        try (var ph = PageHelper.startPage(pageNum, pageSize)) {
            return PageInfo.of(fineTuneMapper.list(spaceId).stream().map(fineTuneEntity -> {
                Long jobId = fineTuneEntity.getJobId();
                JobEntity job = jobMapper.findJobById(jobId);
                fineTuneEntity.getEvalDatasets();
                fineTuneEntity.getTrainDatasets();
                fineTuneEntity.getBaseModelVersionId();
                ModelVo mv = null;
                Long targetModelVersionId = fineTuneEntity.getTargetModelVersionId();
                if (null != targetModelVersionId) {
                    List<ModelVo> modelVos = modelService.findModelByVersionId(List.of(targetModelVersionId));
                    if (!CollectionUtils.isEmpty(modelVos)) {
                        mv = modelVos.get(0);
                    }
                }

                return FineTuneVo.builder()
                        .id(fineTuneEntity.getId())
                        .job(jobConverter.convert(job))
                        .evalDatasets(datasetService.findDatasetsByVersionIds(fineTuneEntity.getTrainDatasets()))
                        .trainDatasets(datasetService.findDatasetsByVersionIds(fineTuneEntity.getEvalDatasets()))
                        .targetModel(mv)
                        .build();
            }).collect(Collectors.toList()));
        }
    }

    public void evalFt(List<Long> evalDatasetIds, Long runtimeId, String handerSpec, Map<String, String> envs) {

    }

    @Transactional
    public void releaseFt(Long ftId, String modelName, User user) {
        FineTuneEntity ft = fineTuneMapper.findById(ftId);
        if (null == ft) {
            throw new SwNotFoundException(ResourceType.FINE_TUNE, "fine tune not found");
        }
        Long targetModelVersionId = ft.getTargetModelVersionId();
        if (null == targetModelVersionId) {
            throw new SwNotFoundException(ResourceType.FINE_TUNE, "target model has not been generated yet");
        }
        ModelVersionEntity modelVersion = modelDao.getModelVersion(targetModelVersionId.toString());
        if (!modelVersion.getDraft()) {
            throw new SwValidationException(
                    ValidSubject.MODEL,
                    "model has been released to modelId: " + modelVersion.getModelId()
            );
        }
        Long modelId;
        if (!StringUtils.hasText(modelName) || modelVersion.getModelName().equals(modelName)) {
            modelId = modelVersion.getModelId();
        } else {
            //release to a new model
            FineTuneSpaceEntity ftSpace = fineTuneSpaceMapper.findById(ft.getSpaceId());
            Long projectId = ftSpace.getProjectId();
            BundleEntity modelEntity = this.modelDao.findByNameForUpdate(modelName, projectId);
            if (null == modelEntity) {
                //create model
                ModelEntity model = ModelEntity.builder()
                        .ownerId(user.getId())
                        .projectId(projectId)
                        .modelName(modelName)
                        .build();
                modelDao.add(model);
                modelId = model.getId();
            } else {
                modelId = modelEntity.getId();
            }
        }
        // update model version model id to new model and set draft to false
        modelDao.releaseModelVersion(targetModelVersionId, modelId);

    }

    public void attachTargetModel(Long id, ModelVersionEntity modelVersionEntity) {
        fineTuneMapper.updateTargetModel(id, modelVersionEntity.getId());
    }

    private void checkFeatureEnabled() throws StarwhaleApiException {
        if (!this.featuresProperties.isFineTuneEnabled()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.FINE_TUNE, "fine-tune feature is disabled"),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
