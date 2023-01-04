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

import ai.starwhale.mlops.api.protocol.job.ModelServingVo;
import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.common.IdConverter;
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
import com.google.protobuf.Api;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletResponse;
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
    private final IdConverter idConverter;


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
            ModelServingTokenValidator modelServingTokenValidator,
            IdConverter idConverter
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
        this.idConverter = idConverter;
    }

    public ModelServingVo create(
            String projectUrl,
            String modelVersionUrl,
            String runtimeVersionUrl,
            String resourcePool
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
                .jobStatus(JobStatus.CREATED)
                .resourcePool(resourcePool)
                .build();

        modelServingMapper.add(entity);

        var services = modelServingMapper.list(projectId, modelVersionId, runtimeVersionId, resourcePool);
        if (services.size() != 1) {
            // this can not happen
            throw new SwProcessException(SwProcessException.ErrorType.DB, "duplicate entries, size " + services.size());
        }
        var id = services.get(0).getId();

        log.info("Model serving job has been created. ID={}", id);

        try {
            deploy(runtime, model, projectId.toString(), user, id);
        } catch (ApiException e) {
            log.error(e.getResponseBody(), e);
            throw new SwProcessException(SwProcessException.ErrorType.SYSTEM, e.getResponseBody(), e);
        }

        var idStr = idConverter.convert(id);
        var uri = getServiceBaseUri(id);
        return ModelServingVo.builder().id(idStr).baseUri(uri).build();
    }

    private void deploy(
            RuntimeVersionEntity runtime,
            ModelVersionEntity model,
            String project,
            User owner,
            long id
    ) throws ApiException {
        var name = getServiceName(id);

        // TODO: refactor image generation
        var image = runtime.getImage();
        if (systemSettingService.getSystemSetting() != null
                && systemSettingService.getSystemSetting().getDockerSetting() != null
                && systemSettingService.getSystemSetting().getDockerSetting().getRegistry() != null
        ) {
            image = new DockerImage(image)
                    .resolve(systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
        }


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
                "SW_MODEL_SERVING_BASE_URI", getServiceBaseUri(id),
                // see https://github.com/star-whale/starwhale/blob/c1d85ab98045a95ab3c75a89e7af56a17e966714/client/starwhale/utils/__init__.py#L51
                "SW_PRODUCTION", "1"
        );
        var ss = k8sJobTemplate.renderModelServingOrch(envs, image, name);
        try {
            k8sClient.deployStatefulSet(ss);
        } catch (ApiException e) {
            if (e.getCode() != HttpServletResponse.SC_CONFLICT) {
                throw e;
            }
            // exists, ignore exception
            return;
        }

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

    public static String getServiceBaseUri(long id) {
        return String.format("/gateway/%s/%d", MODEL_SERVICE_PREFIX, id);
    }
}
