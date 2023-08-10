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

package ai.starwhale.mlops.domain.evaluation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protobuf.Job.JobVo;
import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeVo;
import ai.starwhale.mlops.api.protobuf.User.UserVo;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.mapper.ViewConfigMapper;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EvaluationServiceTest {

    private EvaluationService service;
    private JobDao jobDao;
    private ViewConfigMapper viewConfigMapper;
    private JobConverter jobConvertor;

    @BeforeEach
    public void setUp() {
        UserService userService = mock(UserService.class);
        given(userService.currentUserDetail()).willReturn(User.builder().id(1L).build());
        ProjectService projectService = mock(ProjectService.class);
        given(projectService.getProjectId(same("1"))).willReturn(1L);

        service = new EvaluationService(
                userService,
                projectService,
                jobDao = mock(JobDao.class),
                viewConfigMapper = mock(ViewConfigMapper.class),
                new IdConverter(),
                new ViewConfigConverter(),
                jobConvertor = mock(JobConverter.class),
                new JobStatusMachine()
        );
    }

    @Test
    public void testGetViewConfig() {
        given(viewConfigMapper.findViewConfig(same(1L), same("config")))
                .willReturn(ViewConfigEntity.builder()
                        .id(1L)
                        .configName("config")
                        .content("content1")
                        .createdTime(new Date())
                        .build());
        var res = service.getViewConfig(
                ConfigQuery.builder().projectUrl("1").name("config").build()
        );

        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("name", is("config"))),
                is(hasProperty("content", is("content1")))
        ));

    }

    @Test
    public void testCreateViewConfig() {
        given(viewConfigMapper.createViewConfig(argThat(
                config -> config.getProjectId() == 1
                        && config.getConfigName().equals("config1")
                        && config.getContent().equals("content1")
        ))).willReturn(1);
        ConfigRequest request = new ConfigRequest();
        request.setName("config1");
        request.setContent("content1");

        var res = service.createViewConfig("1", request);
        assertThat(res, is(true));

        res = service.createViewConfig("2", request);
        assertThat(res, is(false));

        request.setName("config2");
        res = service.createViewConfig("1", request);
        assertThat(res, is(false));
    }

    @Test
    public void testListEvaluationSummary() {
        given(jobDao.listJobs(same(1L), any()))
                .willReturn(List.of(
                        JobEntity.builder()
                                .id(1L)
                                .project(ProjectEntity.builder().id(1L).projectName("p1").build())
                                .jobStatus(JobStatus.PAUSED)
                                .build(),
                        JobEntity.builder()
                                .id(2L)
                                .project(ProjectEntity.builder().id(1L).projectName("p1").build())
                                .jobStatus(JobStatus.SUCCESS)
                                .build()
                ));
        given(jobConvertor.convert(any(JobEntity.class)))
                .willAnswer(invocation -> {
                    JobEntity entity = invocation.getArgument(0);
                    return JobVo.newBuilder()
                            .setId(String.valueOf(entity.getId()))
                            .setUuid("uuid" + entity.getId())
                            .setModelName("model" + entity.getId())
                            .addAllDatasets(List.of("1", "2", "3"))
                            .setRuntime(RuntimeVo.newBuilder().setName("runtime" + entity.getId()).build())
                            .setDevice("device" + entity.getId())
                            .setDeviceAmount(3)
                            .setCreatedTime(10L)
                            .setStopTime(11L)
                            .setDuration(1L)
                            .setOwner(UserVo.newBuilder().setName("owner" + entity.getId()).build())
                            .setJobStatus(JobStatus.SUCCESS)
                            .build();
                });
        var res = service.listEvaluationSummary("1", null, new PageParams(1, 5));
        assertEquals(1, res.getPageNum());
        assertEquals(2, res.getTotal());
        var list = res.getList();
        assertEquals(2, list.size());
        var item = list.get(0);
        assertEquals("1", item.getId());
        assertEquals("uuid1", item.getUuid());
        assertEquals("model1", item.getModelName());
        assertEquals("runtime1", item.getRuntime());
        assertEquals("device1", item.getDevice());
        assertEquals("1,2,3", item.getDatasets());
        assertEquals(1L, item.getDuration());
        assertEquals("owner1", item.getOwner());
        assertEquals(JobStatus.SUCCESS, item.getJobStatus());
    }
}
