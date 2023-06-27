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
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.datastore.ListTablesRequest;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.configuration.security.ProjectNameExtractorDataStoreMixed;
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

    @BeforeEach
    public void setup() {
        projectNameExtractorDataStoreMixed = new ProjectNameExtractorDataStoreMixed("api/v1", objectMapper);
    }

    @Test
    public void testNonDataStore() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("api/v1/project/p/abdc/adsf/asd2w?xdcoia032=12&sad=32");
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        Assertions.assertIterableEquals(Set.of("p"), strings);
    }

    @Test
    public void testDataStoreList() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("api/v1/datastore/listTables");
        when(request.getInputStream()).thenReturn(new DelegatingServletInputStream(
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(new ListTablesRequest("project/x")))));
        Set<String> strings = projectNameExtractorDataStoreMixed.extractProjectName(request);
        Assertions.assertIterableEquals(Set.of("x"), strings);
    }

    @Test
    public void testDataStoreUpdate() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("api/v1/datastore/updateTable");
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
        when(request.getRequestURI()).thenReturn("api/v1/datastore/queryTable");
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
        when(request.getRequestURI()).thenReturn("api/v1/datastore/scanTable");
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

}
