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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.ModelServingTokenValidator;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import ai.starwhale.mlops.schedule.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.k8s.ResourceOverwriteSpec;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1StatefulSetStatus;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ModelServingServiceTest {
    private ModelServingService svc;
    private final ModelServingMapper modelServingMapper = mock(ModelServingMapper.class);
    private final RuntimeDao runtimeDao = mock(RuntimeDao.class);
    private final ProjectManager projectManager = mock(ProjectManager.class);
    private final ModelDao modelDao = mock(ModelDao.class);
    private final UserService userService = mock(UserService.class);
    private final K8sClient k8sClient = mock(K8sClient.class);
    private final K8sJobTemplate k8sJobTemplate = mock(K8sJobTemplate.class);
    private final RuntimeMapper runtimeMapper = mock(RuntimeMapper.class);
    private final ModelMapper modelMapper = mock(ModelMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final RunTimeProperties runTimeProperties = mock(RunTimeProperties.class);
    private final ModelServingTokenValidator modelServingTokenValidator = mock(ModelServingTokenValidator.class);

    @BeforeEach
    public void setUp() {
        svc = new ModelServingService(
                modelServingMapper,
                runtimeDao,
                projectManager,
                modelDao,
                userService,
                k8sClient,
                k8sJobTemplate,
                runtimeMapper,
                modelMapper,
                systemSettingService,
                runTimeProperties,
                "inst",
                modelServingTokenValidator,
                new IdConverter(),
                3600,
                1
        );

        var user = User.builder().id(1L).name("starwhale").build();
        when(userService.currentUserDetail()).thenReturn(user);
        when(projectManager.getProjectId(anyString())).thenReturn(2L);

        Mockito.doAnswer(inv -> {
            ModelServingEntity entity = inv.getArgument(0);
            entity.setId(7L);
            return null;
        }).when(modelServingMapper).add(any());

        var runtime = RuntimeEntity.builder().runtimeName("rt").build();
        when(runtimeMapper.find(any())).thenReturn(runtime);

        var model = ModelEntity.builder().modelName("md").build();
        when(modelMapper.find(any())).thenReturn(model);

        var pypi = mock(RunTimeProperties.Pypi.class);
        when(runTimeProperties.getPypi()).thenReturn(pypi);
        when(pypi.getExtraIndexUrl()).thenReturn("extra-index");
        when(pypi.getIndexUrl()).thenReturn("index");
        when(pypi.getTrustedHost()).thenReturn("trusted-host");

        when(modelServingTokenValidator.getToken(any(), any())).thenReturn("token");
    }

    @Test
    public void testCreate() throws ApiException {
        var resourcePool = "default";

        var entity = ModelServingEntity.builder()
                .id(7L)
                .projectId(2L)
                .modelVersionId(9L)
                .runtimeVersionId(8L)
                .resourcePool(resourcePool)
                .build();
        when(modelServingMapper.list(2L, 9L, 8L, resourcePool)).thenReturn(List.of(entity));
        var runtimeVer = RuntimeVersionEntity.builder().id(8L).image("img").build();
        when(runtimeDao.getRuntimeVersion("8")).thenReturn(runtimeVer);
        var modelVer = ModelVersionEntity.builder().id(9L).build();
        when(modelDao.getModelVersion("9")).thenReturn(modelVer);
        when(systemSettingService.queryResourcePool("default")).thenReturn(
                ResourcePool.builder().nodeSelector(Map.of("foo", "bar")).build());

        var spec = "---\n"
                + "resources:\n"
                + "- type: \"foo\"\n"
                + "  request: 7.0\n"
                + "  limit: 8.0\n";

        var ss = new V1StatefulSet();
        ss.metadata(new V1ObjectMeta());
        when(k8sClient.deployStatefulSet(any())).thenReturn(ss);
        svc.create("2", "9", "8", resourcePool, spec);

        var rc = RuntimeResource.builder().type("foo").request(7f).limit(8f).build();
        var expectedResource = new ResourceOverwriteSpec(List.of(rc));
        var expectedEnvs = Map.of(
                "SW_PYPI_TRUSTED_HOST", "trusted-host",
                "SW_PYPI_EXTRA_INDEX_URL", "extra-index",
                "SW_PYPI_INDEX_URL", "index",
                "SW_PROJECT", "2",
                "SW_TOKEN", "token",
                "SW_INSTANCE_URI", "inst",
                "SW_MODEL_VERSION", "md/version/9",
                "SW_RUNTIME_VERSION", "rt/version/8",
                "SW_MODEL_SERVING_BASE_URI", "/gateway/model-serving/7",
                "SW_PRODUCTION", "1"
        );
        verify(k8sJobTemplate).renderModelServingOrch(
                "model-serving-7",
                "img",
                expectedEnvs,
                expectedResource,
                Map.of("foo", "bar"));

        verify(k8sClient).deployService(any());
    }


    @Test
    public void testGarbageCollection() throws ApiException {
        final String oldestName = "model-serving-7";
        final String notTheOldestName = "model-serving-8";
        final String noEntityName = "model-serving-9";
        final String maxTtlName = "model-serving-10";

        var now = System.currentTimeMillis();

        final var noStatus = new V1StatefulSet().metadata(new V1ObjectMeta().name("model-serving-1"));
        when(modelServingMapper.find(1L)).thenReturn(
                ModelServingEntity.builder().lastVisitTime(new Date(now - 5 * 1000)).build());
        final var noMeta = new V1StatefulSet().status(new V1StatefulSetStatus());

        var oldEntity = ModelServingEntity.builder().lastVisitTime(new Date(now - 30 * 1000)).build();  // oldest time
        var newEntity = ModelServingEntity.builder().lastVisitTime(new Date(now - 10 * 1000)).build();
        when(modelServingMapper.find(7L)).thenReturn(oldEntity); // the oldest
        when(modelServingMapper.find(8L)).thenReturn(newEntity);
        final var runningStatus = new V1StatefulSetStatus().readyReplicas(1);
        final var oldest = new V1StatefulSet()
                .metadata(new V1ObjectMeta().name(oldestName))
                .status(runningStatus);
        final var shouldNotBeGc = new V1StatefulSet()
                .metadata(new V1ObjectMeta().name(notTheOldestName))
                .status(runningStatus);

        final var noEntity = new V1StatefulSet().metadata(new V1ObjectMeta().name(noEntityName));
        when(modelServingMapper.find(9L)).thenReturn(null);

        final var reachesTheMaxTtl = new V1StatefulSet().metadata(new V1ObjectMeta().name(maxTtlName));
        when(modelServingMapper.find(10L)).thenReturn(
                ModelServingEntity.builder().lastVisitTime(new Date(now - 3601 * 1000)).build());

        final var pending = new V1StatefulSet()
                .status(new V1StatefulSetStatus().readyReplicas(0))
                .metadata(new V1ObjectMeta().name("model-serving-11"));
        when(modelServingMapper.find(11L)).thenReturn(
                ModelServingEntity.builder().lastVisitTime(new Date(now - 2 * 1000)).build());

        var pendingPod = new V1Pod().status(new V1PodStatus().phase("Pending"));
        var or = new V1OwnerReference().kind("StatefulSet").name("model-serving-11");
        pendingPod.metadata(new V1ObjectMeta().ownerReferences(List.of(or)));

        var readyPod1 = new V1Pod().status(new V1PodStatus().phase("Running"));
        var orOld = new V1OwnerReference().kind("StatefulSet").name(oldestName);
        readyPod1.metadata(new V1ObjectMeta().ownerReferences(List.of(orOld)));

        var readyPod2 = new V1Pod().status(new V1PodStatus().phase("Running"));
        var orNew = new V1OwnerReference().kind("StatefulSet").name(notTheOldestName);
        readyPod2.metadata(new V1ObjectMeta().ownerReferences(List.of(orNew)));

        when(k8sClient.getPodList(any())).thenReturn(new V1PodList().items(List.of(pendingPod, readyPod1, readyPod2)));

        var list = new V1StatefulSetList();
        list.setItems(List.of(noStatus, noMeta, oldest, shouldNotBeGc, pending, noEntity, reachesTheMaxTtl));
        when(k8sClient.getStatefulSetList(any())).thenReturn(list);

        var capture = ArgumentCaptor.forClass(String.class);
        svc.gc();
        verify(k8sClient, times(3)).deleteStatefulSet(capture.capture());
        var names = capture.getAllValues();
        assertThat(names, containsInAnyOrder(oldestName, noEntityName, maxTtlName));


        final var theOnlyRunningAndInMinTtl = new V1StatefulSet()
                .metadata(new V1ObjectMeta().name("model-serving-1"))
                .status(runningStatus);
        when(modelServingMapper.find(1L)).thenReturn(
                ModelServingEntity.builder().lastVisitTime(new Date(now - 500)).build());
        list.setItems(List.of(pending, theOnlyRunningAndInMinTtl));
        reset(k8sClient);
        when(k8sClient.getStatefulSetList(any())).thenReturn(list);
        when(k8sClient.getPodList(any())).thenReturn(new V1PodList().items(List.of(pendingPod, readyPod1, readyPod2)));
        svc.gc();
        verify(k8sClient, times(0)).deleteStatefulSet(any());
    }
}
