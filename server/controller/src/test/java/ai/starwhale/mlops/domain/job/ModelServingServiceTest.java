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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.VirtualJobLoader;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.RuntimeTestConstants;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import io.kubernetes.client.openapi.ApiException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ModelServingServiceTest {

    private ModelServingService svc;
    private final ModelServingMapper modelServingMapper = mock(ModelServingMapper.class);
    private final RuntimeDao runtimeDao = mock(RuntimeDao.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ModelDao modelDao = mock(ModelDao.class);
    private final UserService userService = mock(UserService.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);

    private JobServiceForWeb jobService;

    private VirtualJobLoader virtualJobLoader;

    private JobSpecParser jobSpecParser;

    @BeforeEach
    public void setUp() {
        systemSettingService.updateSetting("---\n"
                                                   + "dockerSetting:\n"
                                                   + "  registryForPull: \"\"\n"
                                                   + "  registryForPush: \"\"\n"
                                                   + "  userName: \"\"\n"
                                                   + "  password: \"\"\n"
                                                   + "  insecure: true\n"
                                                   + "resourcePoolSetting:\n"
                                                   + "- name: \"default\"\n"
                                                   + "  nodeSelector: \n"
                                                   + "    foo: \"bar\"\n"
                                                   + "  resources:\n"
                                                   + "  - name: \"cpu\"\n"
                                                   + "    max: null\n"
                                                   + "    min: null\n"
                                                   + "    defaults: 5.0");
        virtualJobLoader = new VirtualJobLoader(null);
        jobSpecParser = new JobSpecParser();
        jobService = mock(JobServiceForWeb.class);
        svc = new ModelServingService(
                modelServingMapper,
                runtimeDao,
                projectService,
                modelDao,
                userService,
                systemSettingService,
                new IdConverter(),
                3600,
                1,
                20,
                jobService, virtualJobLoader, jobSpecParser
        );

        var user = User.builder().id(1L).name("starwhale").build();
        when(userService.currentUserDetail()).thenReturn(user);
        when(projectService.getProjectId(anyString())).thenReturn(2L);
        when(projectService.findProject(anyString())).thenReturn(Project.builder().id(2L).name("p").build());

        Mockito.doAnswer(inv -> {
            ModelServingEntity entity = inv.getArgument(0);
            entity.setId(7L);
            return null;
        }).when(modelServingMapper).add(any());

        ResourcePool resourcePool = mock(ResourcePool.class);
        when(resourcePool.allowUser(any())).thenReturn(true);
        when(resourcePool.getName()).thenReturn("rp");
        Mockito.doAnswer(inv -> inv.getArgument(0)).when(resourcePool).validateAndPatchResource(anyList());
        when(systemSettingService.queryResourcePool(anyString())).thenReturn(resourcePool);
    }

    @Test
    public void testCreate() throws ApiException {
        var resourcePool = "default";

        var entity = ModelServingEntity.builder()
                .id(7L)
                .projectId(2L)
                .jobStatus(JobStatus.FAIL)
                .modelVersionId(9L)
                .runtimeVersionId(8L)
                .spec("")
                .resourcePool(resourcePool)
                .build();
        when(modelServingMapper.list(2L, 9L, 8L, resourcePool)).thenReturn(List.of(entity));
        var runtimeVer = RuntimeVersionEntity.builder()
                .id(8L)
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .versionName("rt-8")
                .build();
        when(runtimeDao.getRuntimeVersion("8")).thenReturn(runtimeVer);
        var modelVer = ModelVersionEntity.builder().id(9L).versionName("mp-9").build();
        when(modelDao.getModelVersion("9")).thenReturn(modelVer);

        var spec = "---\n"
                + "resources:\n"
                + "- type: \"cpu\"\n"
                + "  request: 7.0\n"
                + "  limit: 8.0\n"
                + "envVars:\n"
                + " \"a\": \"b\"\n";

        svc.create("2", "9", "8", resourcePool, spec);
        verify(jobService).createJob(
                eq("p"),
                eq("9"),
                eq(null),
                eq("8"),
                eq("model online evaluation"),
                eq("rp"),
                eq(null),
                eq("---\n"
                           + "- concurrency: 1\n"
                           + "  resources:\n"
                           + "  - type: \"cpu\"\n"
                           + "    request: 7.0\n"
                           + "    limit: 8.0\n"
                           + "  env:\n"
                           + "  - name: \"a\"\n"
                           + "    value: \"b\"\n"
                           + "  replicas: 1\n"
                           + "  expose: 8080\n"
                           + "  job_name: \"online_eval\"\n"
                           + "  name: \"online_eval\"\n"
                           + "  show_name: \"online_eval\"\n"
                           + "  require_dataset: false\n"),
                eq(JobType.BUILT_IN),
                eq(null),
                eq(false),
                eq(null),
                eq(null)
        );


    }


    @Test
    public void testGarbageCollection() {
        //test expired service
        var servingEntity = ModelServingEntity.builder()
                .id(7L)
                .projectId(2L)
                .jobId(1L)
                .modelVersionId(9L)
                .runtimeVersionId(8L)
                .resourcePool("rp")
                .jobStatus(JobStatus.RUNNING)
                .lastVisitTime(new Date(System.currentTimeMillis() - 3800 * 1000))
                .build();
        when(modelServingMapper.findByStatusIn(JobStatus.RUNNING)).thenReturn(List.of(servingEntity));
        svc.gc();
        verify(jobService).cancelJob("1");

        //test not expired service
        servingEntity.setLastVisitTime(new Date(System.currentTimeMillis()));
        when(modelServingMapper.findByStatusIn(JobStatus.RUNNING)).thenReturn(List.of(servingEntity));
        reset(jobService);
        svc.gc();
        verify(jobService, times(0)).cancelJob("1");

        //test one new job waiting and force killing an existing service
        var createdEntity = ModelServingEntity.builder()
                .id(7L)
                .projectId(2L)
                .jobId(1L)
                .modelVersionId(9L)
                .runtimeVersionId(8L)
                .resourcePool("rp")
                .jobStatus(JobStatus.CREATED)
                .lastVisitTime(new Date(System.currentTimeMillis() - 380 * 1000))
                .build();
        servingEntity.setLastVisitTime(new Date(System.currentTimeMillis() - 1000 * 3));
        when(modelServingMapper.findByStatusIn(JobStatus.RUNNING)).thenReturn(List.of(servingEntity));
        when(modelServingMapper.findByStatusIn(
                JobStatus.CREATED,
                JobStatus.READY
        )).thenReturn(List.of(createdEntity));
        reset(jobService);
        svc.gc();
        verify(jobService, times(1)).cancelJob("1");

    }

    @Test
    public void testGetStatus() {
        var entity = ModelServingEntity.builder()
                .id(7L)
                .projectId(2L)
                .jobId(1L)
                .jobStatus(JobStatus.RUNNING)
                .modelVersionId(9L)
                .runtimeVersionId(8L)
                .resourcePool("rp")
                .build();
        when(modelServingMapper.find(7L)).thenReturn(entity);
        var status = svc.getStatus(7L);
        Assertions.assertEquals("RUNNING", status.getEvents());
    }
}
