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

package ai.starwhale.mlops.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.datastore.ListTablesRequest;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.configuration.security.ProjectNameExtractorDataStoreMixed;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.report.ReportDao;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class ProjectNameExtractorDataStoreMixedTest {

    ProjectNameExtractorDataStoreMixed projectNameExtractorDataStoreMixed;
    ObjectMapper objectMapper = new ObjectMapper();
    ProjectService projectService = mock(ProjectService.class);
    JobDao jobDao = mock(JobDao.class);
    ModelDao modelDao = mock(ModelDao.class);
    DatasetDao datasetDao = mock(DatasetDao.class);
    RuntimeDao runtimeDao = mock(RuntimeDao.class);
    ReportDao reportDao = mock(ReportDao.class);

    @BeforeEach
    public void setup() {
        projectNameExtractorDataStoreMixed = new ProjectNameExtractorDataStoreMixed(
                "/api/v1",
                objectMapper,
                projectService,
                new IdConverter(),
                jobDao,
                modelDao,
                datasetDao,
                runtimeDao,
                reportDao);
    }

    @Test
    public void testNonDataStore() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/project/p/abdc/adsf/asd2w?xdcoia032=12&sad=32");
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        Assertions.assertIterableEquals(Set.of("p"), strings);
    }

    @Test
    public void testDataStoreList() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/datastore/listTables");
        when(request.getInputStream()).thenReturn(new DelegatingServletInputStream(
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(new ListTablesRequest("project/x")))));
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        Assertions.assertIterableEquals(Set.of("x"), strings);
    }

    @Test
    public void testDataStoreUpdate() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/datastore/updateTable");
        UpdateTableRequest value = new UpdateTableRequest();
        value.setTableName("project/x");
        when(request.getInputStream()).thenReturn(
                new DelegatingServletInputStream(new ByteArrayInputStream(objectMapper.writeValueAsBytes(
                        value))));
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        Assertions.assertIterableEquals(Set.of("x"), strings);
    }

    @Test
    public void testDataStoreQuery() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/datastore/queryTable");
        QueryTableRequest value = new QueryTableRequest();
        value.setTableName("project/x");
        when(request.getInputStream()).thenReturn(
                new DelegatingServletInputStream(new ByteArrayInputStream(objectMapper.writeValueAsBytes(
                        value))));
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        Assertions.assertIterableEquals(Set.of("x"), strings);
    }

    @Test
    public void testDataStoreScan() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/datastore/scanTable");
        ScanTableRequest value = new ScanTableRequest();
        TableDesc e1 = new TableDesc();
        e1.setTableName("project/x");
        TableDesc e2 = new TableDesc();
        e2.setTableName("project/y");
        value.setTables(List.of(e1, e2));
        when(request.getInputStream()).thenReturn(
                new DelegatingServletInputStream(new ByteArrayInputStream(objectMapper.writeValueAsBytes(
                        value))));
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        assertThat(Set.of("x", "y")).hasSameElementsAs(strings);
    }

    @Test
    public void testCheckResourceOwnerShip() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        // test url can not match
        for (var uri : List.of(
                "/api/v1/no-project/p1/runtime/r1/foo/bar?a=b",
                "/api/v1/project//p1/runtime/r1/foo/bar?a=b",
                "/api/v1/project/p1/no-runtime/r1/foo/bar?a=b",
                "/api/v1/project/p1/runtime//r1/foo/bar?a=b"
        )) {
            when(request.getRequestURI()).thenReturn(uri);
            projectNameExtractorDataStoreMixed.checkResourceOwnerShip(request);
        }

        when(projectService.findProject("p1")).thenReturn(Project.builder().id(1L).build());
        when(runtimeDao.findById(1L)).thenReturn(RuntimeEntity.builder().projectId(1L).build());
        when(datasetDao.findById(2L)).thenReturn(DatasetEntity.builder().projectId(1L).build());
        when(modelDao.findById(3L)).thenReturn(ModelEntity.builder().projectId(1L).build());
        when(jobDao.findById(4L)).thenReturn(JobEntity.builder().projectId(1L).build());
        when(reportDao.findById(5L)).thenReturn(ReportEntity.builder().projectId(1L).build());

        when(jobDao.findByNameForUpdate("job1", 1L)).thenReturn(JobEntity.builder().projectId(1L).build());

        // test url matches
        for (var uri : List.of(
                "/api/v1/project/p1/runtime/1",
                "/api/v1/project/p1/runtime/1/foo/bar",
                "/api/v1/project/p1/runtime/1/foo/bar/",
                "/api/v1/project/p1/runtime/1/foo/bar?a=b",
                "/api/v1/project/p1/runtime/1/foo/bar?a=b&c=d",
                "/api/v1/project/p1/runtime/1/foo/bar/?a=b&c=d",
                "/api/v1/project/p1/dataset/2",
                "/api/v1/project/p1/model/3",
                "/api/v1/project/p1/job/4",
                "/api/v1/project/p1/job/job1",
                "/api/v1/project/p1/report/5",
                "/api/v1/project/p1/report/5/transfer"
        )) {
            when(request.getRequestURI()).thenReturn(uri);
            projectNameExtractorDataStoreMixed.checkResourceOwnerShip(request);
        }

        when(jobDao.findByNameForUpdate("job2", 1L)).thenReturn(null);

        // test url matches but resource does not exist
        for (var uri : List.of(
                "/api/v1/project/p1/runtime/7",
                "/api/v1/project/p1/runtime/7/foo/bar",
                "/api/v1/project/p1/runtime/7/foo/bar/",
                "/api/v1/project/p1/runtime/7/foo/bar?a=b",
                "/api/v1/project/p1/runtime/7/foo/bar?a=b&c=d",
                "/api/v1/project/p1/runtime/7/foo/bar/?a=b&c=d",
                "/api/v1/project/p1/dataset/8",
                "/api/v1/project/p1/model/9",
                "/api/v1/project/p1/job/10",
                "/api/v1/project/p1/job/job2", // simulate job uuid
                "/api/v1/project/p1/report/11",
                "/api/v1/project/p1/report/11/transfer"
        )) {
            when(request.getRequestURI()).thenReturn(uri);
            Assertions.assertThrows(SwNotFoundException.class,
                    () -> projectNameExtractorDataStoreMixed.checkResourceOwnerShip(request));
        }
        verify(jobDao).findByNameForUpdate("job2", 1L);

        when(runtimeDao.findById(11L)).thenReturn(RuntimeEntity.builder().projectId(2L).build());
        when(datasetDao.findById(12L)).thenReturn(DatasetEntity.builder().projectId(2L).build());
        when(modelDao.findById(13L)).thenReturn(ModelEntity.builder().projectId(2L).build());
        when(jobDao.findById(14L)).thenReturn(JobEntity.builder().projectId(2L).build());
        when(jobDao.findByNameForUpdate("job3", 1L)).thenReturn(null);

        // test url matches but resource does not belong to project
        for (var uri : List.of(
                "/api/v1/project/p1/runtime/11",
                "/api/v1/project/p1/runtime/11/foo/bar",
                "/api/v1/project/p1/runtime/11/foo/bar/",
                "/api/v1/project/p1/runtime/11/foo/bar?a=b",
                "/api/v1/project/p1/runtime/11/foo/bar?a=b&c=d",
                "/api/v1/project/p1/runtime/11/foo/bar/?a=b&c=d",
                "/api/v1/project/p1/dataset/12",
                "/api/v1/project/p1/model/13",
                "/api/v1/project/p1/job/14",
                "/api/v1/project/p1/job/job3", // simulate job uuid
                "/api/v1/project/p1/report/15",
                "/api/v1/project/p1/report/15/transfer"
        )) {
            when(request.getRequestURI()).thenReturn(uri);
            Assertions.assertThrows(SwNotFoundException.class,
                    () -> projectNameExtractorDataStoreMixed.checkResourceOwnerShip(request));
        }

        // test the resource with name
        for (var uri : List.of(
                "/api/v1/project/p1/runtime/r2",
                "/api/v1/project/p1/runtime/r2/foo/bar",
                "/api/v1/project/p1/runtime/r2/foo/bar/",
                "/api/v1/project/p1/runtime/r2/foo/bar?a=b",
                "/api/v1/project/p1/runtime/r2/foo/bar?a=b&c=d",
                "/api/v1/project/p1/runtime/r2/foo/bar/?a=b&c=d",
                "/api/v1/project/p1/dataset/d2",
                "/api/v1/project/p1/model/m2"
        )) {
            when(request.getRequestURI()).thenReturn(uri);
            // no exception
            projectNameExtractorDataStoreMixed.checkResourceOwnerShip(request);
        }
    }
}
