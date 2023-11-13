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
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVo;
import ai.starwhale.mlops.domain.evaluation.EvaluationFileStorage;
import ai.starwhale.mlops.domain.evaluation.EvaluationService;
import ai.starwhale.mlops.domain.storage.HashNamedObjectStore;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
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
    public void testSignLinks() {
        String pj = "pj";
        String version = "eval-version";
        String uri = "uri";
        String signUrl = "sign-url";
        when(evaluationFileStorage.signLinks(Set.of(uri), 100L)).thenReturn(Map.of(uri, signUrl));
        Assertions.assertEquals(
                Map.of(uri, signUrl),
                controller.signLinks(pj, version, Set.of(uri), 100L).getBody().getData()
        );
    }

    @Test
    public void testHeadHashedBlob() throws IOException {
        String project = "project-id";
        String evalVersion = "eval-version";
        HashNamedObjectStore hashNamedObjectStore = mock(HashNamedObjectStore.class);
        when(evaluationFileStorage.hashObjectStore(project, evalVersion)).thenReturn(hashNamedObjectStore);
        when(hashNamedObjectStore.head("h1")).thenReturn("a");
        when(hashNamedObjectStore.head("h2")).thenReturn(null);
        when(hashNamedObjectStore.head("h3")).thenThrow(IOException.class);
        Assertions.assertTrue(controller.headHashedBlob(project, evalVersion, "h1").getStatusCode().is2xxSuccessful());
        Assertions.assertTrue(controller.headHashedBlob(project, evalVersion, "h2").getStatusCode().is4xxClientError());
        Assertions.assertThrows(
                SwProcessException.class,
                () -> controller.headHashedBlob(project, evalVersion, "h3").getStatusCode().is4xxClientError()
        );

    }

    @Test
    public void testGetHashedBlob() throws IOException {
        String project = "project-id";
        String evalVersion = "eval-version";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(response.getOutputStream())
                .willReturn(new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                    }

                    @Override
                    public void write(int b) {
                        output.write(b);
                    }
                });
        HashNamedObjectStore hashNamedObjectStore = mock(HashNamedObjectStore.class);
        when(evaluationFileStorage.hashObjectStore(project, evalVersion)).thenReturn(hashNamedObjectStore);
        when(hashNamedObjectStore.get("h1")).thenReturn(
                new LengthAbleInputStream(new ByteArrayInputStream("hi content".getBytes(StandardCharsets.UTF_8)), 10));
        when(hashNamedObjectStore.get("h2")).thenThrow(IOException.class);
        controller.getHashedBlob(project, evalVersion, "h1", response);
        assertThat(new String(output.toByteArray()), is("hi content"));
        Assertions.assertThrows(
                SwProcessException.class,
                () -> controller.getHashedBlob(project, evalVersion, "h2", mock(HttpServletResponse.class))
        );

    }
}
