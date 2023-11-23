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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserJobConverterTest {
    private UserJobConverter userJobConverter;

    private final IdConverter idConverter = new IdConverter();
    private ProjectService projectService;
    private ModelService modelService;
    private UserService userService;
    private ModelDao modelDao;
    private RuntimeDao runtimeDao;
    private DatasetDao datasetDao;

    private SystemSettingService systemSettingService;
    private JobSpecParser jobSpecParser;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        projectService = mock(ProjectService.class);
        modelService = mock(ModelService.class);
        userService = mock(UserService.class);
        modelDao = mock(ModelDao.class);
        runtimeDao = mock(RuntimeDao.class);
        datasetDao = mock(DatasetDao.class);
        systemSettingService = mock(SystemSettingService.class);
        jobSpecParser = mock(JobSpecParser.class);
        when(jobSpecParser.parseAndFlattenStepFromYaml(anyString()))
                .thenReturn(List.of(
                        StepSpec.builder()
                                .name("a")
                                .build(),
                        StepSpec.builder()
                                .name("b")
                                .build()
                ));

        when(jobSpecParser.parseStepFromYaml(any(), anyString()))
                .thenReturn(List.of(
                        StepSpec.builder()
                                .name("a")
                                .build(),
                        StepSpec.builder()
                                .name("b")
                                .build()
                ));

        userJobConverter = new UserJobConverter(
                idConverter,
                projectService,
                modelService,
                userService,
                modelDao,
                runtimeDao,
                datasetDao,
                jobSpecParser,
                systemSettingService
        );
    }

    @Test
    public void testConvertToUserJobRequest() {
        var projectId = "1";
        var req = new JobRequest();
        req.setResourcePool("test");
        //.handler("mnist.evaluator:MNISTInference.cmp")
        req.setHandler("handler");
        req.setDevMode(false);

        // old version request
        // invalid version url, this must be numeric
        req.setModelVersionUrl("model url");
        req.setRuntimeVersionUrl("2");
        req.setDatasetVersionUrls("3,4");
        assertThrowsExactly(StarwhaleApiException.class, () -> userJobConverter.convert(projectId, req));

        // valid version url
        req.setModelVersionUrl("1");
        var resp = userJobConverter.convert(projectId, req);
        assertEquals("test", resp.getResourcePool());
        assertEquals("handler", resp.getHandler());
        assertFalse(resp.isDevMode());

        assertEquals(1L, resp.getModelVersionId());
        assertEquals(2L, resp.getRuntimeVersionId());
        assertEquals(List.of(3L, 4L), resp.getDatasetVersionIds());

        // new version request
        req.setModelVersionUrl(null);
        req.setModelVersionId("1");
        req.setRuntimeVersionUrl(null);
        req.setRuntimeVersionId("2");
        req.setDatasetVersionUrls(null);
        req.setDatasetVersionIds(List.of("3", "4"));

        resp = userJobConverter.convert(projectId, req);
        assertEquals("test", resp.getResourcePool());
        assertEquals("handler", resp.getHandler());
        assertFalse(resp.isDevMode());

        assertEquals(1L, resp.getModelVersionId());
        assertEquals(2L, resp.getRuntimeVersionId());
        assertEquals(List.of(3L, 4L), resp.getDatasetVersionIds());

        // test no runtime
        req.setRuntimeVersionId(null);
        req.setRuntimeVersionUrl(null);
        assertThrows(StarwhaleApiException.class, () -> userJobConverter.convert(projectId, req));

        // test built-in runtime
        var modelVersionEntity = ModelVersionEntity.builder()
                .id(1L)
                .modelId(11L)
                .builtInRuntime("the built-in runtime version name")
                .build();
        when(modelDao.findVersionById(1L)).thenReturn(modelVersionEntity);
        when(modelDao.findById(11L)).thenReturn(ModelEntity.builder().projectId(42L).build());
        when(runtimeDao.getRuntimeByName(Constants.SW_BUILT_IN_RUNTIME, 42L)).thenReturn(
                RuntimeEntity.builder().id(12L).build());
        when(runtimeDao.findVersionByNameAndBundleId("the built-in runtime version name", 12L)).thenReturn(
                RuntimeVersionEntity.builder().id(7L).build());
        resp = userJobConverter.convert(projectId, req);
        assertEquals(7L, resp.getRuntimeVersionId());
    }

    @Test
    public void testConvertToJobFlattenEntityBuilder() {
        var req = UserJobCreateRequest.builder()
                .devMode(false)
                .jobType(JobType.EVALUATION)
                .modelVersionId(1L)
                .runtimeVersionId(2L)
                .datasetVersionIds(List.of(3L, 4L))
                .handler("handler")
                .ttlInSec(100L)
                .user(mock(User.class))
                .build();

        var modelVersionEntity = ModelVersionEntity.builder()
                .id(1L)
                .modelName("model")
                .versionName("model version name")
                .modelId(11L)
                .build();
        when(modelDao.findVersionById(1L)).thenReturn(modelVersionEntity);

        var modelEntity = ModelEntity.builder()
                .id(11L)
                .modelName("model name")
                .projectId(42L)
                .build();
        when(modelDao.findById(11L)).thenReturn(modelEntity);

        var runtimeVersionEntity = RuntimeVersionEntity.builder()
                .id(2L)
                .versionName("runtime version name")
                .runtimeId(12L)
                .build();
        when(runtimeDao.findVersionById(2L)).thenReturn(runtimeVersionEntity);

        var runtimeEntity = RuntimeEntity.builder()
                .id(12L)
                .runtimeName("runtime name")
                .projectId(42L)
                .build();
        when(runtimeDao.findById(12L)).thenReturn(runtimeEntity);

        when(projectService.findProject(42L)).thenReturn(Project.builder().name("project42").build());
        when(projectService.findProject(43L)).thenReturn(Project.builder().name("project43").build());

        var datasetVersion1 = DatasetVersion.builder()
                .id(3L)
                .datasetName("dataset name 1")
                .versionName("dataset version name 1")
                .datasetId(13L)
                .projectId(42L)
                .build();
        var datasetVersion2 = DatasetVersion.builder()
                .id(4L)
                .datasetName("dataset name 2")
                .versionName("dataset version name 2")
                .datasetId(14L)
                .projectId(43L)
                .build();

        when(datasetDao.getDatasetVersion(3L)).thenReturn(datasetVersion1);
        when(datasetDao.getDatasetVersion(4L)).thenReturn(datasetVersion2);

        var flattenEntity = userJobConverter.convert(req).build();

        assertEquals(req.getUser(), flattenEntity.getOwner());
        assertEquals("project/42/runtime/12/version/2", flattenEntity.getRuntimeUri());
        assertEquals("project/project42/runtime/runtime name/version/runtime version name",
                flattenEntity.getRuntimeUriForView());
        assertEquals("runtime name", flattenEntity.getRuntimeName());
        assertEquals(2L, flattenEntity.getRuntimeVersionId());
        assertEquals("runtime version name", flattenEntity.getRuntimeVersionValue());
        assertEquals("model name", flattenEntity.getModelName());
        assertEquals(1L, flattenEntity.getModelVersionId());
        assertEquals("model version name", flattenEntity.getModelVersionValue());
        assertEquals("project/42/model/11/version/1", flattenEntity.getModelUri());
        assertEquals("project/project42/model/model name/version/model version name",
                flattenEntity.getModelUriForView());
        assertEquals(List.of("project/42/dataset/13/version/3", "project/43/dataset/14/version/4"),
                flattenEntity.getDatasets());
        assertEquals(Set.of(3L, 4L), flattenEntity.getDatasetIdVersionMap().keySet());
        assertEquals("project/project42/dataset/dataset name 1/version/dataset version name 1,"
                        + "project/project43/dataset/dataset name 2/version/dataset version name 2",
                flattenEntity.getDatasetsForView());
        assertFalse(flattenEntity.isDevMode());


        // test without runtime
        req.setRuntimeVersionId(null);
        assertThrows(StarwhaleApiException.class, () -> userJobConverter.convert(req).build());
    }
}