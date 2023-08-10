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


import ai.starwhale.mlops.api.protobuf.Dataset.DatasetVo;
import ai.starwhale.mlops.api.protobuf.Job.ExposedLinkVo;
import ai.starwhale.mlops.api.protobuf.Job.ExposedType;
import ai.starwhale.mlops.api.protobuf.Job.JobVo;
import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.api.protobuf.Model.ModelVo;
import ai.starwhale.mlops.api.protobuf.Model.StepSpec;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.user.converter.UserVoConverter;
import ai.starwhale.mlops.exception.ConvertException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.function.Consumer4;

@Slf4j
@Component
public class JobConverter {

    private final IdConverter idConvertor;
    private final RuntimeService runtimeService;
    private final DatasetService datasetService;
    private final ModelService modelService;
    private final DatasetDao datasetDao;
    private final SystemSettingService systemSettingService;
    private final HotJobHolder hotJobHolder;
    private final JobSpecParser jobSpecParser;
    private final int devPort;
    private final WebServerInTask webServerInTask;
    private final UserVoConverter userVoConverter;

    public JobConverter(
            IdConverter idConvertor,
            RuntimeService runtimeService, DatasetService datasetService, ModelService modelService,
            DatasetDao datasetDao,
            SystemSettingService systemSettingService, HotJobHolder hotJobHolder,
            JobSpecParser jobSpecParser,
            @Value("${sw.task.dev-port}") int devPort,
            WebServerInTask webServerInTask,
            UserVoConverter userVoConverter
    ) {
        this.idConvertor = idConvertor;
        this.runtimeService = runtimeService;
        this.datasetService = datasetService;
        this.modelService = modelService;
        this.datasetDao = datasetDao;
        this.systemSettingService = systemSettingService;
        this.hotJobHolder = hotJobHolder;
        this.jobSpecParser = jobSpecParser;
        this.devPort = devPort;
        this.webServerInTask = webServerInTask;
        this.userVoConverter = userVoConverter;
    }

    private ModelVo findModelByVersionIds(Long versionId) {
        List<ModelVo> modelVos = modelService.findModelByVersionId(List.of(versionId));
        if (CollectionUtils.isEmpty(modelVos) || modelVos.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and model");
        }
        return modelVos.get(0);
    }

    private RuntimeVo findRuntimeByVersionIds(Long versionId) {
        List<RuntimeVo> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(List.of(versionId));
        if (CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and runtime");
        }
        return runtimeByVersionIds.get(0);
    }

    private List<DatasetVo> findDatasetVersionsByJobId(Long jobId) {
        var ids = datasetDao.listDatasetVersionIdsOfJob(jobId);
        return datasetService.findDatasetsByVersionIds(ids);
    }

    /**
     * generateJobExposedLinks generate exposed links for job
     * only running job should generate exposed links
     *
     * @param jobId job id
     * @return exposed links
     */
    private List<ExposedLinkVo> generateJobExposedLinks(Long jobId) {
        var exposed = new ArrayList<ExposedLinkVo>();
        var jobs = hotJobHolder.ofIds(List.of(jobId));
        if (CollectionUtils.isEmpty(jobs)) {
            return exposed;
        }
        var job = jobs.stream().findAny().get();

        // only running job should generate exposed links
        var jobStatus = job.getStatus();
        if (jobStatus != JobStatus.RUNNING) {
            return exposed;
        }
        // check if job has exposed port in step spec
        var stepSpecStr = job.getStepSpec();
        if (!StringUtils.hasText(stepSpecStr)) {
            return exposed;
        }

        List<StepSpec> stepSpecs;
        try {
            stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(stepSpecStr);
        } catch (JsonProcessingException e) {
            return exposed;
        }
        if (CollectionUtils.isEmpty(stepSpecs)) {
            return exposed;
        }

        var steps = job.getSteps();

        Consumer4<Task, Integer, ExposedType, String> addRunningTask = (task, port, type, name) -> {
            if (task.getStatus() != TaskStatus.RUNNING) {
                return;
            }
            var ip = task.getIp();
            if (!StringUtils.hasText(ip)) {
                return;
            }
            var link = webServerInTask.generateGatewayUrl(task.getId(), task.getIp(), port);
            exposed.add(ExposedLinkVo.newBuilder().setType(type).setName(name).setLink(link).build());
        };

        // dev mode
        if (job.isDevMode()) {
            job.getSteps().stream().flatMap(s -> s.getTasks().stream())
                    .forEach(task -> addRunningTask.accept(task, devPort, ExposedType.DEV_MODE, DevWay.VS_CODE.name()));
        }

        // web handler
        // note that the web handler may not accessible if in dev mode
        // we add the web handler link to the exposed links anyway
        // so that user can click the link to open the web handler
        stepSpecs.forEach(stepSpec -> {
            if (!stepSpec.hasExpose() || stepSpec.getExpose() <= 0) {
                return;
            }
            var exposedPort = stepSpec.getExpose();
            var step = steps.stream().filter(s -> s.getName().equals(stepSpec.getName())).findAny().orElse(null);
            if (step == null) {
                return;
            }
            var tasks = step.getTasks();
            if (CollectionUtils.isEmpty(tasks)) {
                return;
            }
            var name = StringUtils.hasText(stepSpec.getShowName()) ? stepSpec.getShowName() : stepSpec.getName();
            tasks.forEach(t -> addRunningTask.accept(t, exposedPort, ExposedType.WEB_HANDLER, name));
        });

        return exposed;
    }

    public JobVo convert(JobEntity jobEntity) throws ConvertException {
        var runtime = findRuntimeByVersionIds(jobEntity.getRuntimeVersionId());
        var datasetList = findDatasetVersionsByJobId(jobEntity.getId());
        var datasetVersions = datasetList.stream().map(ds -> ds.getVersion().getName()).collect(Collectors.toList());
        Long pinnedTime = jobEntity.getPinnedTime() != null ? jobEntity.getPinnedTime().getTime() : null;

        var builder = JobVo.newBuilder()
                .setId(idConvertor.convert(jobEntity.getId()))
                .setUuid(jobEntity.getJobUuid())
                .setOwner(userVoConverter.convert(jobEntity.getOwner()))
                .setModelName(jobEntity.getModelName())
                .setModelVersion(jobEntity.getModelVersion().getVersionName())
                .setModel(findModelByVersionIds(jobEntity.getModelVersionId()))
                .setJobName(extractJobName(jobEntity.getStepSpec()))
                .setCreatedTime(jobEntity.getCreatedTime().getTime())
                .setRuntime(runtime)
                .setBuiltinRuntime(runtime.getVersion().getName()
                                           .equals(jobEntity.getModelVersion().getBuiltInRuntime()))
                .addAllDatasets(datasetVersions)
                .addAllDatasetList(datasetList)
                .setJobStatus(jobEntity.getJobStatus())
                .setStopTime(jobEntity.getFinishedTime().getTime())
                .setResourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()).getName())
                .addAllExposedLinks(generateJobExposedLinks(jobEntity.getId()));

        if (StringUtils.hasText(jobEntity.getComment())) {
            builder.setComment(jobEntity.getComment());
        }
        if (jobEntity.getDurationMs() != null) {
            builder.setDuration(jobEntity.getDurationMs());
        }
        if (pinnedTime != null) {
            builder.setPinnedTime(pinnedTime);
        }
        return builder.build();
    }

    private String extractJobName(String stepSpecStr) {
        if (StringUtils.hasText(stepSpecStr)) {
            List<StepSpec> stepSpecs;
            try {
                stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(stepSpecStr);
                var spec = stepSpecs.stream().findFirst();
                if (spec.isPresent()) {
                    return spec.get().getJobName();
                }
            } catch (JsonProcessingException e) {
                log.error("parse step spec error: {}", e.getMessage(), e);
            }
        }
        return "";
    }

}
