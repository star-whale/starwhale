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

package ai.starwhale.mlops.domain.job;

import static ai.starwhale.mlops.exception.SwValidationException.ValidSubject.ONLINE_EVAL;

import ai.starwhale.mlops.api.protocol.job.ModelServingStatusVo;
import ai.starwhale.mlops.api.protocol.job.ModelServingVo;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.ModelServing;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.ModelServingSpec;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.VirtualJobLoader;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class ModelServingService {

    private final ModelServingMapper modelServingMapper;
    private final UserService userService;
    private final ProjectService projectService;
    private final ModelDao modelDao;
    private final RuntimeDao runtimeDao;
    private final SystemSettingService systemSettingService;
    private final IdConverter idConverter;

    private final long maxTtlSec;
    private final long minTtlSec;
    private final long waitTolerationSec;

    private final JobServiceForWeb jobServiceForWeb;

    private final VirtualJobLoader virtualJobLoader;

    private final JobSpecParser jobSpecParser;

    public ModelServingService(
            ModelServingMapper modelServingMapper,
            RuntimeDao runtimeDao,
            ProjectService projectService,
            ModelDao modelDao,
            UserService userService,
            SystemSettingService systemSettingService,
            IdConverter idConverter,
            @Value("${sw.online-eval.max-time-to-live-in-seconds}") long maxTtlSec,
            @Value("${sw.online-eval.min-time-to-live-in-seconds}") long minTtlSec,
            @Value("${sw.online-eval.max-time-to-wait-in-seconds}") long waitTolerationSec,
            JobServiceForWeb jobServiceForWeb,
            VirtualJobLoader virtualJobLoader,
            JobSpecParser jobSpecParser
    ) {
        this.modelServingMapper = modelServingMapper;
        this.runtimeDao = runtimeDao;
        this.projectService = projectService;
        this.modelDao = modelDao;
        this.userService = userService;
        this.systemSettingService = systemSettingService;
        this.idConverter = idConverter;
        this.maxTtlSec = maxTtlSec;
        this.minTtlSec = minTtlSec;
        this.waitTolerationSec = waitTolerationSec;
        this.jobServiceForWeb = jobServiceForWeb;
        this.virtualJobLoader = virtualJobLoader;
        this.jobSpecParser = jobSpecParser;
    }

    @Transactional
    public ModelServingVo create(
            String projectUrl,
            String modelVersionUrl,
            String runtimeVersionUrl,
            String resourcePool,
            String spec
    ) {
        User user = userService.currentUserDetail();
        Project project = projectService.findProject(projectUrl);
        var runtime = runtimeDao.getRuntimeVersion(runtimeVersionUrl);
        var model = modelDao.getModelVersion(modelVersionUrl);

        ModelServingSpec modelServingSpec = null;
        String orderedSpecStr = null;
        if (StringUtils.isNotEmpty(spec)) {
            try {
                modelServingSpec = ModelServingSpec.fromYamlString(spec);
                orderedSpecStr = modelServingSpec.dumps();
            } catch (JsonProcessingException e) {
                log.error("parse spec failed", e);
                var swExp = new SwValidationException(ONLINE_EVAL, "failed to parse spec", e);
                throw new StarwhaleApiException(swExp, HttpStatus.BAD_REQUEST);
            }
        }

        if (StringUtils.isEmpty(resourcePool)) {
            resourcePool = ResourcePool.DEFAULT_NAME;
        }
        var pool = systemSettingService.queryResourcePool(resourcePool);
        if (pool == null) {
            var swExp = new SwValidationException(ONLINE_EVAL, "resource pool not found");
            throw new StarwhaleApiException(swExp, HttpStatus.BAD_REQUEST);
        }
        if (!pool.allowUser(user.getId())) {
            var swExp = new SwValidationException(ONLINE_EVAL, "user not allowed to use this resource pool");
            throw new StarwhaleApiException(swExp, HttpStatus.BAD_REQUEST);
        }

        long id;
        synchronized (this) {
            ModelServingEntity targetService = null;
            var services = modelServingMapper.list(project.getId(), model.getId(), runtime.getId(), resourcePool);
            if (services != null && !services.isEmpty()) {
                // try getting the exactly same service
                // only care about `spec` for now
                for (var service : services) {
                    if (Objects.equals(service.getSpec(), orderedSpecStr)
                            && Set.of(JobStatus.RUNNING, JobStatus.CREATED, JobStatus.READY)
                            .contains(service.getJobStatus())) {
                        targetService = service;
                        break;
                    }
                }
            }

            if (targetService == null) {
                Long jobId = createJob(modelVersionUrl, runtimeVersionUrl, pool, project, modelServingSpec);
                targetService = ModelServingEntity.builder()
                        .ownerId(user.getId())
                        .runtimeVersionId(runtime.getId())
                        .jobId(jobId)
                        .projectId(project.getId())
                        .modelVersionId(model.getId())
                        .jobStatus(JobStatus.CREATED)
                        .resourcePool(resourcePool)
                        .lastVisitTime(new Date())
                        .spec(orderedSpecStr)
                        .build();
                modelServingMapper.add(targetService);
            } else {
                // update last visit time, prevents garbage collected
                modelServingMapper.updateLastVisitTime(targetService.getId(), new Date());
            }

            id = targetService.getId();
        }

        log.info("Model serving job has been created. ID={}", id);
        var idStr = idConverter.convert(id);
        var uri = getServiceBaseUri(id);
        return ModelServingVo.builder().id(idStr).baseUri(uri).build();
    }

    private Long createJob(
            String modelVersionUrl,
            String runtimeVersionUrl,
            ResourcePool resourcePool,
            Project project,
            ModelServingSpec modelServingSpec
    ) {
        List<StepSpec> stepSpecs;
        try {
            String onlineEvalSteps = virtualJobLoader.loadJobStepSpecs("online_eval");
            stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(onlineEvalSteps);
        } catch (JsonProcessingException e) {
            throw new SwValidationException(
                    ValidSubject.SETTING,
                    "online_eval spec not valid in your $SW_JOB_VIRTUAL_SPECS_PATH"
            );
        } catch (IOException e) {
            throw new SwValidationException(
                    ValidSubject.SETTING,
                    "online_eval spec not found in your $SW_JOB_VIRTUAL_SPECS_PATH"
            );
        }
        if (CollectionUtils.isEmpty(stepSpecs)) {
            throw new SwValidationException(
                    ValidSubject.SETTING,
                    "online_eval spec is empty in your $SW_JOB_VIRTUAL_SPECS_PATH"
            );
        }
        List userEnvs = List.of();
        if (null != modelServingSpec && null != modelServingSpec.getEnvVars()) {
            userEnvs = modelServingSpec.getEnvVars().entrySet().stream().map(entry -> new Env(
                    entry.getKey(),
                    entry.getValue()
            )).collect(Collectors.toList());
        }

        List<RuntimeResource> resources = null;
        // get the resources from user input
        if (modelServingSpec != null && modelServingSpec.getResources() != null) {
            resources = modelServingSpec.getResources();
        }
        resources = resourcePool.validateAndPatchResource(resources);

        List<RuntimeResource> finalResources = resources;
        List finalUserEnvs = userEnvs;
        stepSpecs.forEach(stepSpec -> {
            List<Env> envs = stepSpec.getEnv();
            if (null != envs) {
                envs.addAll(finalUserEnvs);
            } else {
                stepSpec.setEnv(finalUserEnvs);
            }
            stepSpec.setResources(finalResources);
        });

        String stepSpecOverWrites;
        try {
            stepSpecOverWrites = Constants.yamlMapper.writeValueAsString(stepSpecs);
        } catch (JsonProcessingException e) {
            throw new SwProcessException(ErrorType.SYSTEM, "error occurs while writing ds build step specs to string",
                                         e
            );
        }
        return jobServiceForWeb.createJob(
                project.getName(),
                modelVersionUrl,
                null,
                runtimeVersionUrl,
                "model online evaluation",
                resourcePool.getName(),
                null,
                stepSpecOverWrites,
                JobType.BUILT_IN,
                null,
                false,
                null,
                null
        );

    }

    public static String getServiceBaseUri(long id) {
        return String.format("/gateway/%s/%d", ModelServing.MODEL_SERVICE_PREFIX, id);
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void gc() {
        List<ModelServingEntity> runningServings = modelServingMapper.findByStatusIn(JobStatus.RUNNING);
        boolean clearAtLeastOne = clearAgedJobs(runningServings);
        if (clearAtLeastOne) {
            return;
        }
        var now = System.currentTimeMillis();
        List<ModelServingEntity> mayBeGced = runningServings.stream()
                .filter(rs -> now - rs.getLastVisitTime().getTime() > minTtlSec * 1000)
                .sorted(Comparator.comparing(ModelServingEntity::getLastVisitTime))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mayBeGced)) {
            return;
        }
        List<ModelServingEntity> notScheduledServings = modelServingMapper.findByStatusIn(
                        JobStatus.CREATED,
                        JobStatus.READY
                )
                .stream()
                .filter(rs -> now - rs.getLastVisitTime().getTime() > waitTolerationSec)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(notScheduledServings)) {
            return;
        }
        jobServiceForWeb.cancelJob(mayBeGced.get(0).getJobId().toString());
    }

    private boolean clearAgedJobs(List<ModelServingEntity> runningServings) {
        if (CollectionUtils.isEmpty(runningServings)) {
            return false;
        }
        boolean clearAtLeastOne = false;
        for (var rs : runningServings) {
            var now = System.currentTimeMillis();
            var last = rs.getLastVisitTime().getTime();
            if (now - last > maxTtlSec * 1000) {
                log.info(
                        "delete online eval {} when it reaches the max TTL (since {})",
                        rs.getJobId().toString(),
                        last
                );
                jobServiceForWeb.cancelJob(rs.getJobId().toString());
                clearAtLeastOne = true;
            }
        }
        return clearAtLeastOne;
    }

    public ModelServingStatusVo getStatus(Long id) {
        ModelServingEntity modelServingEntity = modelServingMapper.find(id);
        return ModelServingStatusVo.builder().events(modelServingEntity.getJobStatus().toString()).build();
    }
}
