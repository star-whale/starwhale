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

package ai.starwhale.mlops.domain.trash;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.report.ReportDao;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.bo.TrashQuery;
import ai.starwhale.mlops.domain.trash.mapper.TrashMapper;
import ai.starwhale.mlops.domain.trash.po.TrashPo;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TrashServiceTest {

    private TrashService service;
    private TrashMapper trashMapper;
    private UserService userService;
    private ProjectService projectService;
    private ModelDao modelDao;
    private DatasetDao datasetDao;
    private RuntimeDao runtimeDao;
    private JobDao jobDao;
    private ReportDao reportDao;

    @BeforeEach
    public void setUp() {
        trashMapper = mock(TrashMapper.class);
        userService = mock(UserService.class);
        projectService = mock(ProjectService.class);
        modelDao = mock(ModelDao.class);
        datasetDao = mock(DatasetDao.class);
        runtimeDao = mock(RuntimeDao.class);
        jobDao = mock(JobDao.class);
        reportDao = mock(ReportDao.class);

        service = new TrashService(trashMapper,
                userService,
                projectService,
                modelDao,
                datasetDao,
                runtimeDao,
                jobDao,
                reportDao,
                new IdConverter());
    }


    @Test
    public void testListTrash() {
        User starwhale = User.builder().id(1L).name("starwhale").build();
        User test = User.builder().id(2L).name("test").build();
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(userService.getUserId(same("starwhale")))
                .willReturn(1L);
        given(userService.loadUserById(same(1L)))
                .willReturn(starwhale);
        given(userService.loadUserById(same(2L)))
                .willReturn(test);
        given(trashMapper.list(same(1L), any(), any(), any()))
                .willReturn(List.of(
                        TrashPo.builder()
                                .id(1L)
                                .trashName("model1")
                                .operatorId(1L)
                                .trashType("MODEL")
                                .updatedTime(new Date())
                                .createdTime(new Date())
                                .retention(new Date())
                                .build(),
                        TrashPo.builder()
                                .id(2L)
                                .trashName("dataset1")
                                .operatorId(2L)
                                .trashType("DATASET")
                                .updatedTime(new Date())
                                .createdTime(new Date())
                                .retention(new Date())
                                .build()
                ));

        var res = service.listTrash(TrashQuery.builder().projectUrl("1").build(),
                PageParams.builder().build(),
                OrderParams.builder().build());
        System.out.println(res);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("total", is(2L)),
                hasProperty("list", allOf(
                        hasItem(hasProperty("trashedBy", is("starwhale"))),
                        hasItem(hasProperty("trashedBy", is("test")))
                ))
        ));
    }

    @Test
    public void testMoveToRecycleBin() {
        given(modelDao.findById(same(1L)))
                .willReturn(ModelEntity.builder().id(1L).modelName("model1").modifiedTime(new Date()).build());
        given(datasetDao.findById(same(1L)))
                .willReturn(DatasetEntity.builder().id(2L).datasetName("dataset1").modifiedTime(new Date()).build());
        given(runtimeDao.findById(same(1L)))
                .willReturn(RuntimeEntity.builder().id(3L).runtimeName("runtime1").modifiedTime(new Date()).build());
        given(jobDao.findById(same(1L)))
                .willReturn(JobEntity.builder().id(4L).jobUuid("job1").modifiedTime(new Date()).build());
        given(reportDao.findById(same(1L)))
                .willReturn(ReportEntity.builder().id(5L).uuid("report1").modifiedTime(new Date()).build());
        given(trashMapper.insert(any(TrashPo.class)))
                .willAnswer(invocation -> {
                    TrashPo po = invocation.getArgument(0);
                    po.setId(po.getObjectId());
                    return null;
                });

        User user = User.builder().id(1L).name("starwhale").build();
        var res = service.moveToRecycleBin(
                Trash.builder()
                        .type(Type.MODEL)
                        .objectId(1L)
                        .projectId(1L)
                        .build(), user);
        assertThat(res, is(1L));
        res = service.moveToRecycleBin(
                Trash.builder()
                        .type(Type.DATASET)
                        .objectId(1L)
                        .projectId(1L)
                        .build(), user);
        assertThat(res, is(2L));
        res = service.moveToRecycleBin(
                Trash.builder()
                        .type(Type.RUNTIME)
                        .objectId(1L)
                        .projectId(1L)
                        .build(), user);
        assertThat(res, is(3L));
        res = service.moveToRecycleBin(
                Trash.builder()
                        .type(Type.EVALUATION)
                        .objectId(1L)
                        .projectId(1L)
                        .build(), user);
        assertThat(res, is(4L));
        res = service.moveToRecycleBin(
                Trash.builder()
                        .type(Type.REPORT)
                        .objectId(1L)
                        .projectId(1L)
                        .build(), user);
        assertThat(res, is(5L));
    }

    @Test
    public void testRecover() {
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(projectService.getProjectId(same("2")))
                .willReturn(2L);
        given(trashMapper.find(same(1L)))
                .willReturn(TrashPo.builder()
                        .id(1L)
                        .projectId(1L)
                        .operatorId(1L)
                        .objectId(1L)
                        .trashType("MODEL")
                        .trashName("model1")
                        .retention(new Date())
                        .updatedTime(new Date())
                        .createdTime(new Date())
                        .build());
        given(trashMapper.find(same(2L)))
                .willReturn(TrashPo.builder()
                        .id(1L)
                        .projectId(1L)
                        .operatorId(1L)
                        .objectId(1L)
                        .trashType("DATASET")
                        .trashName("dataset1")
                        .retention(new Date())
                        .updatedTime(new Date())
                        .createdTime(new Date())
                        .build());
        given(trashMapper.find(same(3L)))
                .willReturn(TrashPo.builder()
                        .id(1L)
                        .projectId(1L)
                        .operatorId(1L)
                        .objectId(1L)
                        .trashType("RUNTIME")
                        .trashName("runtime1")
                        .retention(new Date())
                        .updatedTime(new Date())
                        .createdTime(new Date())
                        .build());
        given(trashMapper.find(same(4L)))
                .willReturn(TrashPo.builder()
                        .id(1L)
                        .projectId(1L)
                        .operatorId(1L)
                        .objectId(1L)
                        .trashType("EVALUATION")
                        .trashName("job1")
                        .retention(new Date())
                        .updatedTime(new Date())
                        .createdTime(new Date())
                        .build());
        given(trashMapper.find(same(4L)))
                .willReturn(TrashPo.builder()
                        .id(1L)
                        .projectId(1L)
                        .operatorId(1L)
                        .objectId(1L)
                        .trashType("REPORT")
                        .trashName("report1")
                        .retention(new Date())
                        .updatedTime(new Date())
                        .createdTime(new Date())
                        .build());

        given(modelDao.findDeletedBundleById(same(1L)))
                .willReturn(ModelEntity.builder().id(1L).modelName("model1").modifiedTime(new Date()).build());
        given(datasetDao.findDeletedBundleById(same(1L)))
                .willReturn(DatasetEntity.builder().id(1L).datasetName("dataset1").modifiedTime(new Date()).build());
        given(jobDao.findDeletedBundleById(same(1L)))
                .willReturn(JobEntity.builder().id(1L).jobUuid("job1").modifiedTime(new Date()).build());
        given(reportDao.findDeletedBundleById(same(1L)))
                .willReturn(ReportEntity.builder().id(1L).uuid("report1").modifiedTime(new Date()).build());

        given(runtimeDao.findByNameForUpdate(same("runtime1"), any()))
                .willReturn(RuntimeEntity.builder().id(1L).runtimeName("runtime1").modifiedTime(new Date()).build());
        given(jobDao.findByNameForUpdate(same("job1"), any()))
                .willReturn(JobEntity.builder().id(1L).jobUuid("job1").modifiedTime(new Date()).build());
        given(reportDao.findByNameForUpdate(same("report1"), any()))
                .willReturn(ReportEntity.builder().id(1L).uuid("report1").modifiedTime(new Date()).build());

        given(modelDao.recover(any())).willReturn(true);
        given(datasetDao.recover(any())).willReturn(true);
        given(runtimeDao.recover(any())).willReturn(true);
        given(jobDao.recover(any())).willReturn(true);
        given(reportDao.recover(any())).willReturn(true);

        var res = service.recover("1", 1L);
        assertThat(res, is(true));

        assertThrows(SwValidationException.class, () -> service.recover("2", 1L));

        res = service.recover("1", 2L);
        assertThat(res, is(true));

        assertThrows(SwValidationException.class, () -> service.recover("1", 3L));

        assertThrows(SwValidationException.class, () -> service.recover("1", 4L));

    }

    @Test
    public void testDelete() {
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(trashMapper.find(same(1L)))
                .willReturn(TrashPo.builder().projectId(1L).build());
        given(trashMapper.delete(same(1L)))
                .willReturn(1);

        var res = service.deleteTrash("1", 1L);
        assertThat(res, is(true));
        assertThrows(SwValidationException.class, () -> service.recover("2", 1L));
        assertThrows(SwValidationException.class, () -> service.recover("1", 2L));
    }
}
