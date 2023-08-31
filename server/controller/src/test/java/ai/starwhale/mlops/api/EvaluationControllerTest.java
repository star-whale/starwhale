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

package ai.starwhale.mlops.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.evaluation.AttributeVo;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVo;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.evaluation.EvaluationFileStorage;
import ai.starwhale.mlops.domain.evaluation.EvaluationService;
import ai.starwhale.mlops.domain.evaluation.bo.SummaryFilter;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.Page;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class EvaluationControllerTest {

    private EvaluationController controller;
    private EvaluationService evaluationService;
    private EvaluationFileStorage evaluationFileStorage;

    @BeforeEach
    public void setUp() {
        controller = new EvaluationController(
                evaluationService = mock(EvaluationService.class),
                evaluationFileStorage = mock(EvaluationFileStorage.class));
    }

    @Test
    public void testListAttributes() {
        given(evaluationService.listAttributeVo())
                .willReturn(List.of(AttributeVo.builder().name("attr").type("string").build()));

        var resp = controller.listAttributes("");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("name", is("attr"))))
        ));
    }

    @Test
    public void testGetViewConfig() {
        given(evaluationService.getViewConfig(argThat(query ->
                query.getProjectUrl().equals("1") && query.getName().equals("config"))))
                .willReturn(ConfigVo.builder().name("name1").content("content1").build());

        var resp = controller.getViewConfig("1", "config");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                isA(ConfigVo.class),
                is(hasProperty("name", is("name1"))),
                is(hasProperty("content", is("content1")))
        ));
    }

    @Test
    public void testCreateViewConfig() {
        given(evaluationService.createViewConfig(same("p1"),
                argThat(req -> req.getName().equals("config") && req.getContent().equals("content"))))
                .willReturn(true);

        ConfigRequest request = new ConfigRequest();
        request.setName("config");
        request.setContent("content");
        var resp = controller.createViewConfig("p1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.createViewConfig("p2", request));
    }

    @Test
    public void testListEvaluationSummary() {
        given(evaluationService.listEvaluationSummary(
                same("p1"),
                any(SummaryFilter.class),
                any(PageParams.class)
        )).willAnswer(invocation -> {
            PageParams pageParams = invocation.getArgument(2);
            try (Page<SummaryVo> page = new Page<>(pageParams.getPageNum(), pageParams.getPageSize())) {
                return page.toPageInfo();
            }
        });

        var resp = controller.listEvaluationSummary(
                "p1", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(1))),
                is(hasProperty("pageSize", is(5)))
        ));
    }
}
