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
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.ModelServing;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.ModelServingTokenValidator;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.spec.ModelServingSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceEventHolder;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModelServingService {

    private final ModelServingMapper modelServingMapper;
    private final UserService userService;
    private final ProjectService projectService;
    private final ModelDao modelDao;
    private final RuntimeDao runtimeDao;
    private final K8sClient k8sClient;
    private final K8sJobTemplate k8sJobTemplate;
    private final RuntimeMapper runtimeMapper;
    private final ModelMapper modelMapper;
    private final SystemSettingService systemSettingService;
    private final String instanceUri;
    private final RunTimeProperties runTimeProperties;
    private final ModelServingTokenValidator modelServingTokenValidator;
    private final IdConverter idConverter;
    private final ResourceEventHolder resourceEventHolder;

    private final long maxTtlSec;
    private final long minTtlSec;
    private final Map<Long, Boolean> availableWorkloads;

    private static final Pattern modelServingNamePattern =
            Pattern.compile(ModelServing.MODEL_SERVICE_PREFIX + "-(\\d+)");

    public ModelServingService(
            ModelServingMapper modelServingMapper,
            RuntimeDao runtimeDao,
            ProjectService projectService,
            ModelDao modelDao,
            UserService userService,
            K8sClient k8sClient,
            K8sJobTemplate k8sJobTemplate,
            RuntimeMapper runtimeMapper,
            ModelMapper modelMapper,
            SystemSettingService systemSettingService,
            RunTimeProperties runTimeProperties,
            ModelServingTokenValidator modelServingTokenValidator,
            IdConverter idConverter,
            ResourceEventHolder resourceEventHolder,
            @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.online-eval.max-time-to-live-in-seconds}") long maxTtlSec,
            @Value("${sw.online-eval.min-time-to-live-in-seconds}") long minTtlSec
    ) {
        this.modelServingMapper = modelServingMapper;
        this.runtimeDao = runtimeDao;
        this.projectService = projectService;
        this.modelDao = modelDao;
        this.userService = userService;
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
        this.runtimeMapper = runtimeMapper;
        this.modelMapper = modelMapper;
        this.systemSettingService = systemSettingService;
        this.runTimeProperties = runTimeProperties;
        this.modelServingTokenValidator = modelServingTokenValidator;
        this.idConverter = idConverter;
        this.resourceEventHolder = resourceEventHolder;
        this.instanceUri = instanceUri;
        this.maxTtlSec = maxTtlSec;
        this.minTtlSec = minTtlSec;

        availableWorkloads = new ConcurrentHashMap<>();
        this.k8sClient.watchStatefulSet(new ResourceEventHandler<>() {
            private Long getServiceId(V1StatefulSet ss) {
                if (ss.getMetadata() == null || ss.getMetadata().getName() == null) {
                    return null;
                }
                return getServiceIdFromName(ss.getMetadata().getName());
            }

            @Override
            public void onAdd(V1StatefulSet ss) {
                var id = getServiceId(ss);
                if (id == null) {
                    return;
                }
                availableWorkloads.put(id, true);
            }

            @Override
            public void onUpdate(V1StatefulSet oldObj, V1StatefulSet newObj) {
                // we do not care about the update event, do nothing
            }

            @Override
            public void onDelete(V1StatefulSet ss, boolean deletedFinalStateUnknown) {
                var id = getServiceId(ss);
                if (id == null) {
                    return;
                }
                // this stateful set may be deleted by another reason,
                // so we need to do the deletion both in this event and in the deleteStatefulSet function call
                availableWorkloads.remove(id);
            }
        }, K8sClient.toV1LabelSelector(K8sJobTemplate.starwhaleJobLabel));
    }

    public ModelServingVo create(
            String projectUrl,
            String modelVersionUrl,
            String runtimeVersionUrl,
            String resourcePool,
            String spec
    ) {
        User user = userService.currentUserDetail();
        Long projectId = projectService.getProjectId(projectUrl);
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
            var services = modelServingMapper.list(projectId, model.getId(), runtime.getId(), resourcePool);
            if (services != null && !services.isEmpty()) {
                // try getting the exactly same service
                // only care about `spec` for now
                for (var service : services) {
                    if (Objects.equals(service.getSpec(), orderedSpecStr)) {
                        targetService = service;
                        break;
                    }
                }
            }

            if (targetService == null) {
                targetService = ModelServingEntity.builder()
                        .ownerId(user.getId())
                        .runtimeVersionId(runtime.getId())
                        .projectId(projectId)
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

        try {
            deploy(runtime, model, projectId.toString(), user, id, modelServingSpec, pool);
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
            long id,
            ModelServingSpec modelServingSpec,
            ResourcePool resourcePool
    ) throws ApiException {
        // fast path
        if (availableWorkloads.get(id) != null) {
            // already exists
            return;
        }

        var name = getServiceName(id);

        String builtImage = runtime.getBuiltImage();
        String image = StringUtils.isNotEmpty(builtImage) ? builtImage :
                runtime.getImage(systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPull());

        var rt = runtimeMapper.find(runtime.getRuntimeId());
        var md = modelMapper.find(model.getModelId());

        var envs = new HashMap<String, String>();
        envs.put("SW_RUNTIME_PYTHON_VERSION", runtime.getPythonVersion());
        envs.put("SW_VERSION", runtime.getSwVersion());
        envs.put("SW_RUNTIME_VERSION", String.format("%s/version/%s", rt.getRuntimeName(), runtime.getVersionName()));
        envs.put("SW_MODEL_VERSION", String.format("%s/version/%s", md.getModelName(), model.getVersionName()));
        envs.put("SW_INSTANCE_URI", instanceUri);
        envs.put("SW_TOKEN", modelServingTokenValidator.getToken(owner, id));
        envs.put("SW_PROJECT", project);
        envs.put("SW_PYPI_INDEX_URL", runTimeProperties.getPypi().getIndexUrl());
        envs.put("SW_PYPI_EXTRA_INDEX_URL", runTimeProperties.getPypi().getExtraIndexUrl());
        envs.put("SW_PYPI_TRUSTED_HOST", runTimeProperties.getPypi().getTrustedHost());
        envs.put("SW_PYPI_TIMEOUT", String.valueOf(runTimeProperties.getPypi().getTimeout()));
        envs.put("SW_PYPI_RETRIES", String.valueOf(runTimeProperties.getPypi().getRetries()));
        // see https://github.com/star-whale/starwhale/blob/c1d85ab98045a95ab3c75a89e7af56a17e966714/client/starwhale/utils/__init__.py#L51
        envs.put("SW_MODEL_SERVING_BASE_URI", getServiceBaseUri(id));
        envs.put("SW_PRODUCTION", "1");
        if (null != modelServingSpec && null != modelServingSpec.getEnvVars()) {
            envs.putAll(modelServingSpec.getEnvVars());
        }

        List<RuntimeResource> resources = null;
        // get the resources from user input
        if (modelServingSpec != null && modelServingSpec.getResources() != null) {
            resources = modelServingSpec.getResources();
        }
        resources = resourcePool.validateAndPatchResource(resources);
        log.info("using resource pool {}, patched resources {}", resourcePool, resources);
        var resourceOverwriteSpec = new ResourceOverwriteSpec(resources);

        var nodeSelectors = resourcePool.getNodeSelector();
        var ss = k8sJobTemplate.renderModelServingOrch(name, image, envs, resourceOverwriteSpec, nodeSelectors);
        try {
            ss = k8sClient.deployStatefulSet(ss);
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
        var selector = Map.of(K8sJobTemplate.LABEL_APP, name);
        spec.selector(selector);
        var port = new V1ServicePort();
        port.name("model-serving-port");
        port.protocol("TCP");
        port.port(80);
        port.targetPort(new IntOrString(K8sJobTemplate.ONLINE_EVAL_PORT_IN_POD));
        spec.ports(List.of(port));

        // add owner reference for svc and we can just delete the stateful-set when gc is needed
        var ownerRef = new V1OwnerReference();
        ownerRef.name(name);
        ownerRef.kind(ss.getKind());
        ownerRef.apiVersion(ss.getApiVersion());
        Objects.requireNonNull(ss.getMetadata());
        ownerRef.uid(ss.getMetadata().getUid());
        meta.ownerReferences(List.of(ownerRef));

        // add svc to k8s
        k8sClient.deployService(svc);

        // if operations of svc failed, the gc thread will delete the zombie stateful-set,
        // so we do not need to delete the previous stateful-set when this fails
    }

    public static String getServiceName(long id) {
        return String.format("%s-%d", ModelServing.MODEL_SERVICE_PREFIX, id);
    }

    public static Long getServiceIdFromName(String name) {
        var match = modelServingNamePattern.matcher(name);
        if (match.matches()) {
            return Long.parseLong(match.group(1));
        }
        return null;
    }

    public static String getServiceBaseUri(long id) {
        return String.format("/gateway/%s/%d", ModelServing.MODEL_SERVICE_PREFIX, id);
    }


    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void gc() {
        try {
            synchronized (this) {
                internalGc();
            }
        } catch (ApiException e) {
            log.error("Failed to gc, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
        }
    }

    public void internalGc() throws ApiException {
        var labelSelector = K8sClient.toV1LabelSelector(Map.of(
                K8sJobTemplate.LABEL_WORKLOAD_TYPE,
                K8sJobTemplate.WORKLOAD_TYPE_ONLINE_EVAL
        ));
        var statefulSetList = k8sClient.getStatefulSetList(labelSelector);
        var podList = k8sClient.getPodList(labelSelector);
        var pods = new HashMap<String, V1Pod>();
        for (var pod : podList.getItems()) {
            if (pod.getMetadata() == null || pod.getMetadata().getOwnerReferences() == null) {
                continue;
            }
            for (var ownerRef : pod.getMetadata().getOwnerReferences()) {
                if (ownerRef.getKind() == null || !ownerRef.getKind().equals("StatefulSet")) {
                    continue;
                }
                var name = ownerRef.getName();
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                pods.put(name, pod);
                log.info("found pod {} for stateful set {}", pod.getMetadata().getName(), name);
            }
        }

        boolean hasPending = false;
        Map<Date, V1StatefulSet> mayBeGarbageCollected = new TreeMap<>((t1, t2) -> {
            // oldest at the beginning
            return t1 == t2 ? 0 : t1.before(t2) ? -1 : 1;
        });

        for (var statefulSet : statefulSetList.getItems()) {
            // check if the stateful set is outdated
            var meta = statefulSet.getMetadata();
            if (meta == null || StringUtils.isEmpty(meta.getName())) {
                continue;
            }
            // parse entity id from stateful set name
            var name = meta.getName();
            var id = getServiceIdFromName(name);
            if (id == null) {
                log.warn("can not get entity id from name {}", name);
                continue;
            }

            // check if the record in db
            var entity = modelServingMapper.find(id);
            if (entity == null) {
                // delete the unknown stateful set
                log.info("delete stateful set {} when there is no entry in db", name);
                deleteStatefulSet(name);
                continue;
            }

            var now = System.currentTimeMillis();
            var last = entity.getLastVisitTime().getTime();
            if (now - last > maxTtlSec * 1000) {
                log.info("delete stateful set {} when it reaches the max TTL (since {})", name, last);
                deleteStatefulSet(name);
                continue;
            }

            var status = statefulSet.getStatus();
            // check if the stateful set is pending
            if (status == null) {
                // may have just been deployed, ignore
                log.info("ignore stateful set {} (no status found)", name);
                continue;
            }

            // check if the pod is running
            var pod = pods.get(name);
            if (pod == null) {
                // maybe no enough resources
                log.info("stateful set {} has no matching pod", name);
                hasPending = true;
                continue;
            }
            var podStatus = pod.getStatus();
            // no status found or the status is pending
            if (podStatus == null || (podStatus.getPhase() != null && podStatus.getPhase().equals("Pending"))) {
                log.info("stateful set {} has pending pod", name);
                hasPending = true;
                continue;
            }

            // current replica of online eval is 1, so 0 means not ready
            if (status.getReadyReplicas() == null || status.getReadyReplicas() == 0) {
                log.info("found stateful set {} not ready, won't gc", name);
                continue;
            }

            if (now - last < minTtlSec * 1000) {
                log.info("stateful set {} not reach the TTL (since {}), won't gc", name, last);
                continue;
            }

            mayBeGarbageCollected.put(entity.getLastVisitTime(), statefulSet);
        }

        if (!hasPending) {
            log.info("no pending stateful set, done");
            return;
        }

        // kill the oldest stateful set
        if (mayBeGarbageCollected.isEmpty()) {
            log.info("no stateful set to gc");
            return;
        }
        var key = mayBeGarbageCollected.keySet().iterator().next();
        var oldest = mayBeGarbageCollected.get(key);
        var name = Objects.requireNonNull(oldest.getMetadata()).getName();
        deleteStatefulSet(name);
        log.info("delete stateful set {}", name);
    }

    private void deleteStatefulSet(String name) {
        // mark deleted
        var id = getServiceIdFromName(name);
        if (id != null) {
            availableWorkloads.remove(id);
        }
        try {
            k8sClient.deleteStatefulSet(name);
        } catch (ApiException e) {
            if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                log.info("stateful set {} not found", name);
                return;
            }
            log.error("delete stateful set {} failed, reason {}", name, e.getResponseBody(), e);
        }
    }

    public ModelServingStatusVo getStatus(Long id) {
        var statefulSetName = getServiceName(id);
        // we are now using stateful set with 1 replica
        var podName = statefulSetName + "-0";
        var events = resourceEventHolder.getPodEvents(podName);
        return ModelServingStatusVo.builder().events(events).build();
    }
}
