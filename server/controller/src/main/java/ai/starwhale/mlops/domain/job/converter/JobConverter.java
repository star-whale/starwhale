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


import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.exception.ConvertException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class JobConverter {

    private final IdConverter idConvertor;
    private final RuntimeService runtimeService;
    private final DatasetDao datasetDao;
    private final SystemSettingService systemSettingService;
    private final HotJobHolder hotJobHolder;
    private final JobSpecParser jobSpecParser;
    private final int devPort;
    private final WebServerInTask webServerInTask;

    public JobConverter(
            IdConverter idConvertor,
            RuntimeService runtimeService, DatasetDao datasetDao,
            SystemSettingService systemSettingService, HotJobHolder hotJobHolder,
            JobSpecParser jobSpecParser,
            @Value("${sw.task.dev-port}") int devPort,
            WebServerInTask webServerInTask
    ) {
        this.idConvertor = idConvertor;
        this.runtimeService = runtimeService;
        this.datasetDao = datasetDao;
        this.systemSettingService = systemSettingService;
        this.hotJobHolder = hotJobHolder;
        this.jobSpecParser = jobSpecParser;
        this.devPort = devPort;
        this.webServerInTask = webServerInTask;
    }

    private List<RuntimeVo> findRuntimeByVersionIds(List<Long> versionIds) {
        List<RuntimeVo> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(versionIds);
        if (CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and runtime");
        }
        return runtimeByVersionIds;
    }

    private List<String> findDatasetVersionNamesByJobId(Long jobId) {
        List<DatasetVersion> datasetVersions = datasetDao.listDatasetVersionsOfJob(jobId);
        return datasetVersions.stream()
                .map(DatasetVersion::getVersionName)
                .collect(Collectors.toList());
    }

    /**
     * generateJobExposedLinks generate exposed links for job
     * only running job should generate exposed links
     *
     * @param jobId job id
     * @return exposed links
     */
    private List<String> generateJobExposedLinks(Long jobId) {
        var links = new ArrayList<String>();
        var job = hotJobHolder.getJob(jobId);
        if (null == job) {
            return links;
        }
        // only running job should generate exposed links
        if (job.getStatus() != JobStatus.RUNNING) {
            return links;
        }
        // check if job has exposed port in step spec
        var stepSpecStr = job.getStepSpec();
        if (!StringUtils.hasText(stepSpecStr)) {
            return links;
        }

        List<StepSpec> stepSpecs;
        try {
            stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(stepSpecStr);
        } catch (JsonProcessingException e) {
            return links;
        }
        if (CollectionUtils.isEmpty(stepSpecs)) {
            return links;
        }

        var steps = job.getSteps();

        BiConsumer<Task, Integer> addRunningTask = (task, port) -> {
            if (task.getStatus() != TaskStatus.RUNNING) {
                return;
            }
            var ip = task.getIp();
            if (!StringUtils.hasText(ip)) {
                return;
            }
            var link = webServerInTask.generateGatewayUrl(task.getId(), task.getIp(), port);
            links.add(link);
        };

        // dev mode
        if (job.isDevMode()) {
            job.getSteps().stream().flatMap(s -> s.getTasks().stream())
                    .forEach(task -> addRunningTask.accept(task, devPort));
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
            tasks.forEach(task -> addRunningTask.accept(task, exposedPort));
        });

        return links;
    }

    public JobVo convert(Job job) throws ConvertException {
        var runtimes = findRuntimeByVersionIds(List.of(job.getJobRuntime().getId()));
        var datasets = findDatasetVersionNamesByJobId(job.getId());

        return JobVo.builder()
                .id(idConvertor.convert(job.getId()))
                .uuid(job.getUuid())
                .owner(UserVo.from(job.getOwner(), idConvertor))
                .modelName(job.getModel().getName())
                .modelVersion(job.getModel().getVersion())
                .createdTime(job.getCreatedTime().getTime())
                .runtime(runtimes.get(0))
                .datasets(datasets)
                .jobStatus(job.getStatus())
                .stopTime(job.getFinishedTime().getTime())
                .duration(job.getDurationMs())
                .comment(job.getComment())
                .resourcePool(job.getResourcePool().getName())
                .pinnedTime(job.getPinnedTime() == null ? null : job.getPinnedTime().getTime())
                .exposedLinks(generateJobExposedLinks(job.getId()))
                .build();
    }

    public JobVo convert(JobEntity jobEntity) throws ConvertException {
        var runtimes = findRuntimeByVersionIds(List.of(jobEntity.getRuntimeVersionId()));
        var datasets = findDatasetVersionNamesByJobId(jobEntity.getId());
        Long pinnedTime = jobEntity.getPinnedTime() != null ? jobEntity.getPinnedTime().getTime() : null;

        return JobVo.builder()
                .id(idConvertor.convert(jobEntity.getId()))
                .uuid(jobEntity.getJobUuid())
                .owner(UserVo.fromEntity(jobEntity.getOwner(), idConvertor))
                .modelName(jobEntity.getModelName())
                .modelVersion(jobEntity.getModelVersion().getVersionName())
                .createdTime(jobEntity.getCreatedTime().getTime())
                .runtime(runtimes.get(0))
                .datasets(datasets)
                .jobStatus(jobEntity.getJobStatus())
                .stopTime(jobEntity.getFinishedTime().getTime())
                .duration(jobEntity.getDurationMs())
                .comment(jobEntity.getComment())
                .resourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()).getName())
                .exposedLinks(generateJobExposedLinks(jobEntity.getId()))
                .pinnedTime(pinnedTime)
                .build();
    }

}
