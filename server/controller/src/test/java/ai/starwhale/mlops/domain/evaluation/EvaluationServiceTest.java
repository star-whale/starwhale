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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.mapper.ViewConfigMapper;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
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
                jobConvertor = mock(JobConverter.class)
        );
    }

    @Test
    public void testGetViewConfig() {
        given(viewConfigMapper.findViewConfig(same(1L), same(1L), same("config")))
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
                        Job.builder()
                                .id(1L)
                                .project(Project.builder().id(1L).name("p1").build())
                                .status(JobStatus.PAUSED)
                                .build(),
                        Job.builder()
                                .id(2L)
                                .project(Project.builder().id(1L).name("p1").build())
                                .status(JobStatus.SUCCESS)
                                .build()
                ));
        given(jobConvertor.convert(any(Job.class)))
                .willAnswer(invocation -> {
                    Job entity = invocation.getArgument(0);
                    return JobVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .uuid("uuid" + entity.getId())
                            .modelName("model" + entity.getId())
                            .datasets(List.of("1", "2", "3"))
                            .runtime(RuntimeVo.builder().name("runtime" + entity.getId()).build())
                            .device("device" + entity.getId())
                            .deviceAmount(3)
                            .createdTime(10L)
                            .stopTime(11L)
                            .owner(UserVo.builder().name("owner" + entity.getId()).build())
                            .jobStatus(JobStatus.SUCCESS)
                            .build();
                });
        var res = service.listEvaluationSummary(
                "1", null, new PageParams(1, 5));
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(1))),
                is(hasProperty("total", is(2L))),
                is(hasProperty("list", allOf(
                        is(iterableWithSize(2)),
                        is(hasItem(allOf(
                                        hasProperty("id", is("1")),
                                        hasProperty("uuid", is("uuid1")),
                                        hasProperty("modelName", is("model1")),
                                        hasProperty("runtime", is("runtime1")),
                                        hasProperty("device", is("device1")),
                                        hasProperty("datasets", is("1,2,3")),
                                        hasProperty("duration", is(1L)),
                                        hasProperty("owner", is("owner1")),
                                        hasProperty("jobStatus", is(JobStatus.SUCCESS))
                                ))
                        )
                )))
        ));
    }
}
