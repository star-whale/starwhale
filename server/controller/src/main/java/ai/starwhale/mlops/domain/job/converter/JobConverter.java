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


import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.job.ExposedLinkVo;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.ExposedType;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
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

    public JobConverter(
            IdConverter idConvertor,
            RuntimeService runtimeService, DatasetService datasetService, ModelService modelService,
            DatasetDao datasetDao,
            SystemSettingService systemSettingService, HotJobHolder hotJobHolder,
            JobSpecParser jobSpecParser,
            @Value("${sw.task.dev-port}") int devPort,
            WebServerInTask webServerInTask
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
    }

    private ModelVo findModelByVersionIds(Long versionId) {
        if (null == versionId) {
            return null;
        }
        List<ModelVo> modelVos = modelService.findModelByVersionId(List.of(versionId));
        if (CollectionUtils.isEmpty(modelVos) || modelVos.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and model");
        }
        return modelVos.get(0);
    }

    private RuntimeVo findRuntimeByVersionIds(Long versionId) {
        if (null == versionId) {
            return null;
        }
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
        var stepSpecs = job.getStepSpecs();
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
            exposed.add(ExposedLinkVo.builder().type(type).name(name).link(link).build());
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
            var exposedPort = stepSpec.getExpose();
            if (exposedPort == null || exposedPort <= 0) {
                return;
            }
            var step = steps.stream().filter(s -> s.getName().equals(stepSpec.getName())).findAny().orElse(null);
            if (step == null) {
                return;
            }
            var tasks = step.getTasks();
            if (CollectionUtils.isEmpty(tasks)) {
                return;
            }
            var name = stepSpec.getFriendlyName();
            tasks.forEach(t -> addRunningTask.accept(t, exposedPort, ExposedType.WEB_HANDLER, name));
        });

        return exposed;
    }

    public JobVo convert(JobEntity jobEntity) throws ConvertException {
        var runtime = findRuntimeByVersionIds(jobEntity.getRuntimeVersionId());
        var datasetList = findDatasetVersionsByJobId(jobEntity.getId());
        var datasetVersions = datasetList.stream().map(ds -> ds.getVersion().getName()).collect(Collectors.toList());
        Long pinnedTime = jobEntity.getPinnedTime() != null ? jobEntity.getPinnedTime().getTime() : null;

        return JobVo.builder()
                .id(idConvertor.convert(jobEntity.getId()))
                .uuid(jobEntity.getJobUuid())
                .owner(UserVo.fromEntity(jobEntity.getOwner(), idConvertor))
                .modelName(jobEntity.getModelName())
                .modelVersion(jobEntity.getModelVersion().getVersionName())
                .model(findModelByVersionIds(jobEntity.getModelVersionId()))
                .jobName(extractJobName(jobEntity.getStepSpec()))
                .createdTime(jobEntity.getCreatedTime().getTime())
                .runtime(runtime)
                .builtinRuntime(null == runtime ? null : runtime.getVersion().getName()
                        .equals(jobEntity.getModelVersion().getBuiltInRuntime()))
                .datasets(datasetVersions)
                .datasetList(datasetList)
                .jobStatus(jobEntity.getJobStatus())
                .stopTime(jobEntity.getFinishedTime().getTime())
                .duration(jobEntity.getDurationMs())
                .stepSpec(jobEntity.getStepSpec())
                .comment(jobEntity.getComment())
                .resourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()).getName())
                .exposedLinks(generateJobExposedLinks(jobEntity.getId()))
                .pinnedTime(pinnedTime)
                .build();
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
