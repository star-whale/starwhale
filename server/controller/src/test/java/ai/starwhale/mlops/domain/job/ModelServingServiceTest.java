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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.ModelServingTokenValidator;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import ai.starwhale.mlops.schedule.k8s.K8sJobTemplate;
import io.kubernetes.client.openapi.ApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private final RuntimeVersionMapper runtimeVersionMapper = mock(RuntimeVersionMapper.class);
    private final ModelMapper modelMapper = mock(ModelMapper.class);
    private final ModelVersionMapper modelVersionMapper = mock(ModelVersionMapper.class);
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
                new IdConverter()
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
        var runtimeVer = RuntimeVersionEntity.builder().id(8L).image("img").build();
        when(runtimeDao.getRuntimeVersion(any())).thenReturn(runtimeVer);

        var model = ModelEntity.builder().modelName("md").build();
        when(modelMapper.find(any())).thenReturn(model);
        var modelVer = ModelVersionEntity.builder().id(9L).build();
        when(modelDao.getModelVersion(any())).thenReturn(modelVer);

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
        when(runtimeDao.getRuntimeVersionId("8", null)).thenReturn(8L);
        when(modelDao.getModelVersionId("9", null)).thenReturn(9L);
        svc.create("2", "9", "8", resourcePool);

        verify(k8sJobTemplate).renderModelServingOrch(
                Map.of(
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
                ), "img", "model-serving-7");

        verify(k8sClient).deployService(any());
    }
}
