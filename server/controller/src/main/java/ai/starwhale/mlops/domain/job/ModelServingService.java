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

import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.ModelServingTokenValidator;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import ai.starwhale.mlops.schedule.k8s.K8sJobTemplate;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModelServingService {
    private final ModelServingMapper modelServingMapper;
    private final UserService userService;
    private final ProjectManager projectManager;
    private final ModelDao modelDao;
    private final RuntimeDao runtimeDao;
    private final K8sClient k8sClient;
    private final K8sJobTemplate k8sJobTemplate;
    private final RuntimeMapper runtimeMapper;
    private final RuntimeVersionMapper runtimeVersionMapper;
    private final ModelMapper modelMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final SystemSettingService systemSettingService;
    private final String instanceUri;
    private final RunTimeProperties runTimeProperties;
    private final ModelServingTokenValidator modelServingTokenValidator;


    public static final String MODEL_SERVICE_PREFIX = "model-serving";

    public ModelServingService(
            ModelServingMapper modelServingMapper,
            RuntimeDao runtimeDao,
            ProjectManager projectManager,
            ModelDao modelDao,
            UserService userService,
            K8sClient k8sClient,
            K8sJobTemplate k8sJobTemplate,
            RuntimeMapper runtimeMapper,
            RuntimeVersionMapper runtimeVersionMapper,
            ModelMapper modelMapper,
            ModelVersionMapper modelVersionMapper,
            SystemSettingService systemSettingService,
            RunTimeProperties runTimeProperties,
            @Value("${sw.instance-uri}") String instanceUri,
            ModelServingTokenValidator modelServingTokenValidator
    ) {
        this.modelServingMapper = modelServingMapper;
        this.runtimeDao = runtimeDao;
        this.projectManager = projectManager;
        this.modelDao = modelDao;
        this.userService = userService;
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.modelMapper = modelMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.systemSettingService = systemSettingService;
        this.runTimeProperties = runTimeProperties;
        this.instanceUri = instanceUri;
        this.modelServingTokenValidator = modelServingTokenValidator;
    }

    public Long create(
            String projectUrl,
            String modelVersionUrl,
            String runtimeVersionUrl,
            String resourcePool,
            long ttlInSeconds
    ) {
        User user = userService.currentUserDetail();
        Long projectId = projectManager.getProjectId(projectUrl);
        Long runtimeVersionId = runtimeDao.getRuntimeVersionId(runtimeVersionUrl, null);
        Long modelVersionId = modelDao.getModelVersionId(modelVersionUrl, null);

        // TODO move the deployment logic into the scheduler
        var runtime = runtimeVersionMapper.find(runtimeVersionId);
        var model = modelVersionMapper.find(modelVersionId);

        var entity = ModelServingEntity.builder()
                .ownerId(user.getId())
                .runtimeVersionId(runtimeVersionId)
                .projectId(projectId)
                .modelVersionId(modelVersionId)
                .finishedTime(new Date(System.currentTimeMillis() + ttlInSeconds * 1000))
                .jobStatus(JobStatus.CREATED)
                .resourcePool(resourcePool)
                .build();

        modelServingMapper.add(entity);
        log.info("Model serving job has been created. ID={}", entity.getId());

        try {
            deploy(runtime, model, projectId.toString(), user, entity.getId());
        } catch (ApiException e) {
            log.error(e.getResponseBody(), e);
            throw new SwProcessException(SwProcessException.ErrorType.SYSTEM, e.getResponseBody(), e);
        }

        return entity.getId();
    }

    private void deploy(RuntimeVersionEntity runtime, ModelVersionEntity model, String project, User owner, long id)
            throws ApiException {
        // TODO: refactor image generation
        var image = runtime.getImage();
        if (systemSettingService.getSystemSetting() != null
                && systemSettingService.getSystemSetting().getDockerSetting() != null
                && systemSettingService.getSystemSetting().getDockerSetting().getRegistry() != null
        ) {
            image = new DockerImage(image)
                    .resolve(systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
        }

        var name = getServiceName(id);

        var rt = runtimeMapper.find(runtime.getRuntimeId());
        var md = modelMapper.find(model.getModelId());

        var envs = Map.of(
                "SW_RUNTIME_VERSION", String.format("%s/version/%s", rt.getRuntimeName(), runtime.getId()),
                "SW_MODEL_VERSION", String.format("%s/version/%s", md.getModelName(), model.getId()),
                "SW_INSTANCE_URI", instanceUri,
                "SW_TOKEN", modelServingTokenValidator.getToken(owner, id),
                "SW_PROJECT", project,
                "SW_PYPI_INDEX_URL", runTimeProperties.getPypi().getIndexUrl(),
                "SW_PYPI_EXTRA_INDEX_URL", runTimeProperties.getPypi().getExtraIndexUrl(),
                "SW_PYPI_TRUSTED_HOST", runTimeProperties.getPypi().getTrustedHost(),
                "SW_MODEL_SERVING_BASE_URI", String.format("/gateway/%s/%d", MODEL_SERVICE_PREFIX, id)
        );
        var ss = k8sJobTemplate.renderModelServingOrch(envs, image, name);
        k8sClient.deployStatefulSet(ss);

        var svc = new V1Service();
        var meta = new V1ObjectMeta();
        meta.name(name);
        svc.metadata(meta);
        var spec = new V1ServiceSpec();
        svc.spec(spec);
        var selector = Map.of("app", name);
        spec.selector(selector);
        var port = new V1ServicePort();
        port.name("model-serving-port");
        port.protocol("TCP");
        port.port(80);
        port.targetPort(new IntOrString(8080));
        spec.ports(List.of(port));
        k8sClient.deployService(svc);
        // TODO add owner reference for svc
        // TODO garbage collection when svc fails
    }

    public static String getServiceName(long id) {
        return String.format("%s-%d", MODEL_SERVICE_PREFIX, id);
    }
}
