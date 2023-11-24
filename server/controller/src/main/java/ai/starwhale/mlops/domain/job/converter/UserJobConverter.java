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

package ai.starwhale.mlops.domain.job.converter;


import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class UserJobConverter {
    public static final String FORMATTER_URI_ARTIFACT = "project/%s/%s/%s/version/%s";

    private final IdConverter idConvertor;
    private final ProjectService projectService;
    private final ModelService modelService;
    private final UserService userService;
    private final ModelDao modelDao;
    private final RuntimeDao runtimeDao;
    private final DatasetDao datasetDao;

    private final JobSpecParser jobSpecParser;

    private final SystemSettingService systemSettingService;

    public UserJobConverter(
            IdConverter idConvertor,
            ProjectService projectService,
            ModelService modelService,
            UserService userService,
            ModelDao modelDao,
            RuntimeDao runtimeDao,
            DatasetDao datasetDao,
            JobSpecParser jobSpecParser,
            SystemSettingService systemSettingService
    ) {
        this.idConvertor = idConvertor;
        this.projectService = projectService;
        this.modelService = modelService;
        this.userService = userService;
        this.modelDao = modelDao;
        this.runtimeDao = runtimeDao;
        this.datasetDao = datasetDao;
        this.jobSpecParser = jobSpecParser;
        this.systemSettingService = systemSettingService;
    }

    @NotNull
    public UserJobCreateRequest convert(String projectUrl, JobRequest request) throws SwValidationException {
        var project = projectService.findProject(projectUrl);

        var modelVersionId = findId(request.getModelVersionId(), request.getModelVersionUrl(), "model version");
        if (modelVersionId == null) {
            this.badRequest("model version is required");
        }
        var runtimeVersionId = findId(request.getRuntimeVersionId(), request.getRuntimeVersionUrl(), "runtime version");
        if (runtimeVersionId == null) {
            // try builtin runtime
            var modelVersion = modelDao.findVersionById(modelVersionId);
            if (modelVersion == null) {
                this.badRequest("model version %s not found", modelVersionId);
            }
            var model = modelDao.findById(modelVersion.getModelId());
            if (model == null) {
                this.badRequest("model %s not found", modelVersion.getModelId());
            }

            var runtime = runtimeDao.getRuntimeByName(Constants.SW_BUILT_IN_RUNTIME, model.getProjectId());
            if (runtime == null) {
                this.badRequest("builtin runtime of %s not found", model.getName());
            }
            var runtimeVersion =
                    runtimeDao.findVersionByNameAndBundleId(modelVersion.getBuiltInRuntime(), runtime.getId());
            if (runtimeVersion == null) {
                this.badRequest("builtin runtime version %s not found", modelVersion.getBuiltInRuntime());
            }
            runtimeVersionId = runtimeVersion.getId();

        }
        List<Long> datasets = null;
        if (request.getDatasetVersionIds() == null) {
            var datasetUrls = request.getDatasetVersionUrls();
            if (StringUtils.hasText(datasetUrls)) {
                // convert datasetUrls to datasetIds
                var converted = new ArrayList<Long>();
                for (var url : datasetUrls.split(",")) {
                    var id = findId(null, url, "dataset version");
                    if (id == null) {
                        this.badRequest("dataset version %s not found", url);
                    }
                    converted.add(id);
                }
                datasets = converted;
                log.warn("datasetVersionUrls is deprecated, please use datasetVersionIds instead");
            }
        } else {
            datasets = idConvertor.revertList(request.getDatasetVersionIds());
        }

        List<StepSpec> stepSpecOverWrites = null;
        if (StringUtils.hasText(request.getStepSpecOverWrites())) {
            try {
                stepSpecOverWrites = jobSpecParser.parseAndFlattenStepFromYaml(request.getStepSpecOverWrites());
            } catch (JsonProcessingException e) {
                throw new SwValidationException(ValidSubject.MODEL, "invalid step spec", e);
            }
        }
        return UserJobCreateRequest.builder()
                .project(project)
                .modelVersionId(modelVersionId)
                .runtimeVersionId(runtimeVersionId)
                .datasetVersionIds(datasets)
                .comment(request.getComment())
                .resourcePool(request.getResourcePool())
                .handler(request.getHandler())
                .stepSpecOverWrites(stepSpecOverWrites)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .jobType(request.getType())
                .devMode(request.isDevMode())
                .devWay(request.getDevWay())
                .devPassword(request.getDevPassword())
                .ttlInSec(request.getTimeToLiveInSec())
                .user(userService.currentUserDetail())
                .build();
    }

    public JobFlattenEntity.JobFlattenEntityBuilder convert(UserJobCreateRequest request)
            throws SwValidationException {
        var modelVersion = modelDao.findVersionById(request.getModelVersionId());
        if (null == modelVersion) {
            this.badRequest("model version %s not found", request.getModelVersionId());
        }

        var runtimeVersion = runtimeDao.findVersionById(request.getRuntimeVersionId());
        if (null == runtimeVersion) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "runtime version not found"),
                    HttpStatus.BAD_REQUEST
            );
        }
        var runtime = runtimeDao.findById(runtimeVersion.getRuntimeId());
        var model = modelDao.findById(modelVersion.getModelId());
        var stepSpecOverWrites = request.getStepSpecOverWrites();
        var handler = request.getHandler();
        if ((CollectionUtils.isEmpty(stepSpecOverWrites) && !StringUtils.hasText(handler))
                || (!CollectionUtils.isEmpty(stepSpecOverWrites) && StringUtils.hasText(handler))) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "handler or stepSpec must be provided only one"),
                    HttpStatus.BAD_REQUEST
            );
        }
        List<StepSpec> steps;
        try {
            steps = !CollectionUtils.isEmpty(stepSpecOverWrites)
                    ? stepSpecOverWrites
                    : jobSpecParser.parseStepFromYaml(modelVersion.getJobs(), handler);
            for (var s : steps) {
                s.verifyStepSpecArgs();
            }
        } catch (JsonProcessingException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "failed to parse job step", e), HttpStatus.BAD_REQUEST);
        }
        if (CollectionUtils.isEmpty(steps)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "no stepSpec is configured"), HttpStatus.BAD_REQUEST);
        }

        var pool = systemSettingService.queryResourcePool(request.getResourcePool());
        if (pool != null) {
            for (var step : steps) {
                pool.validateResources(step.getResources());
            }
            if (!pool.allowUser(request.getUser().getId())) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.JOB, "creator is not allowed to use this resource pool"),
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        var datasetIds = request.getDatasetVersionIds();
        List<DatasetVersion> datasets = datasetIds != null ? datasetIds.stream()
                .map(datasetDao::getDatasetVersion)
                .collect(Collectors.toList())
                : List.of();
        var datasetUrls = datasets.stream().map(version -> String.format(FORMATTER_URI_ARTIFACT,
                version.getProjectId(),
                "dataset",
                version.getDatasetId(),
                version.getId())).collect(Collectors.toList());
        var datasetVersionIdMaps = datasets.isEmpty() ? new HashMap<Long, String>()
                : datasets.stream().collect(Collectors.toMap(DatasetVersion::getId, DatasetVersion::getVersionName));

        var datasetsForView = datasets.isEmpty() ? null
                : datasets.stream()
                .map(version -> String.format(FORMATTER_URI_ARTIFACT,
                        projectService.findProject(version.getProjectId()).getName(),
                        "dataset",
                        version.getDatasetName(),
                        version.getVersionName()))
                .collect(Collectors.joining(","));
        var releaseTime = request.getTtlInSec() == null ? null :
                new Date(System.currentTimeMillis() + request.getTtlInSec() * 1000);

        var devMode = request.isDevMode();

        String stepSpecYaml;
        try {
            stepSpecYaml = Constants.yamlMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new SwProcessException(ErrorType.SYSTEM, "serialize stepSpec failed", e);
        }
        return JobFlattenEntity.builder()
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .owner(request.getUser())
                .runtimeUri(String.format(FORMATTER_URI_ARTIFACT, runtime.getProjectId(), "runtime", runtime.getId(),
                        runtimeVersion.getId()))
                .runtimeUriForView(String.format(FORMATTER_URI_ARTIFACT,
                        projectService.findProject(runtime.getProjectId()).getName(),
                        "runtime",
                        runtime.getName(),
                        runtimeVersion.getVersionName()))
                .runtimeName(runtime.getName())
                .runtimeVersionId(runtimeVersion.getId())
                .runtimeVersionValue(runtimeVersion.getVersionName())
                .modelName(model.getName())
                .modelVersion(modelService.findModelVersion(modelVersion.getId()))
                .stepSpec(stepSpecYaml)
                .name(steps.get(0).getJobName())
                .modelVersionId(modelVersion.getId())
                .modelVersionValue(modelVersion.getVersionName())
                .modelUri(String.format(FORMATTER_URI_ARTIFACT, model.getProjectId(), "model", model.getId(),
                        modelVersion.getId()))
                .modelUriForView(String.format(FORMATTER_URI_ARTIFACT,
                        projectService.findProject(model.getProjectId()).getName(),
                        "model",
                        model.getName(),
                        modelVersion.getVersionName()))
                .datasets(datasetUrls)
                .datasetIdVersionMap(datasetVersionIdMaps)
                .datasetsForView(datasetsForView)
                .devMode(request.isDevMode())
                .devWay(devMode ? request.getDevWay() : null)
                .devPassword(devMode ? request.getDevPassword() : null)
                .autoReleaseTime(releaseTime);
    }

    @Nullable
    private Long findId(String id, String url, String nameForMsg) throws SwValidationException {
        if (StringUtils.hasText(id)) {
            return idConvertor.revert(id);
        }
        if (StringUtils.hasText(url)) {
            if (!idConvertor.isId(url)) {
                this.badRequest("%s url must be an id, got %s", nameForMsg, url);
            }
            log.warn("{} url is deprecated, please use id instead", nameForMsg);
            return idConvertor.revert(url);
        }
        return null;
    }

    private void badRequest(String fmt, Object... args) throws SwValidationException {
        var msg = String.format(fmt, args);
        throw new StarwhaleApiException(new SwValidationException(ValidSubject.JOB, msg), HttpStatus.BAD_REQUEST);
    }
}
