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
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeInfoVo;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeViewVo;
import ai.starwhale.mlops.api.protobuf.Runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeRevertRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeTagRequest;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class RuntimeControllerTest {

    private RuntimeController controller;

    private RuntimeService runtimeService;

    @BeforeEach
    public void setUp() {
        runtimeService = mock(RuntimeService.class);

        controller = new RuntimeController(runtimeService);
    }

    @Test
    public void testListRuntime() {
        given(runtimeService.listRuntime(any(RuntimeQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        RuntimeVo.newBuilder().setId("1").build(),
                        RuntimeVo.newBuilder().setId("2").build()
                )));

        var resp = controller.listRuntime("", "3", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(2))
        ));

    }

    @Test
    public void testRevertRuntimeVersion() {
        given(runtimeService.revertVersionTo(same("1"), same("2"), same("3")))
                .willReturn(true);

        RuntimeRevertRequest request = new RuntimeRevertRequest();
        request.setVersionUrl("3");
        var resp = controller.revertRuntimeVersion("1", "2", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setVersionUrl("4");
        assertThrows(StarwhaleApiException.class,
                () -> controller.revertRuntimeVersion("1", "2", request));
    }

    @Test
    public void testDeleteRuntime() {
        given(runtimeService.deleteRuntime(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getRuntimeUrl(), "r1")))).willReturn(true);
        var resp = controller.deleteRuntime("p1", "r1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteRuntime("p2", "r1"));

    }

    @Test
    public void testRecoverRuntime() {
        given(runtimeService.getRuntimeInfo(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getRuntimeUrl(), "r1")
                && Objects.equals(argument.getRuntimeVersionUrl(), "v1")))
        ).willReturn(RuntimeInfoVo.newBuilder().setId("1").build());
        var resp = controller.getRuntimeInfo("p1", "r1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("id", is("1"))
        ));
        resp = controller.getRuntimeInfo("p2", "r1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), nullValue());
    }

    @Test
    public void testListRuntimeVersion() {
        given(runtimeService.listRuntimeVersionHistory(any(RuntimeVersionQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        RuntimeVersionVo.newBuilder().setId("1").build(),
                        RuntimeVersionVo.newBuilder().setId("2").build()
                )));

        var resp = controller.listRuntimeVersion("p1", "r1", "v1", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(2))
        ));
    }

    @Test
    public void testModifyRuntime() {
        given(runtimeService.modifyRuntimeVersion(same("p1"), same("r1"), same("v1"), any()))
                .willReturn(true);

        var resp = controller.modifyRuntime("p1", "r1", "v1", new RuntimeTagRequest());
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThrows(StarwhaleApiException.class,
                () -> controller.modifyRuntime("p2", "r1", "v1", new RuntimeTagRequest()));
    }

    @Test
    public void testUpload() {
        var resp = controller.upload("p1", "r1", "v1", null, new ClientRuntimeRequest());
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testPull() {
        controller.pull("p1", "r1", "v1", null);
    }

    @Test
    public void testHeadRuntime() {
        given(runtimeService.query(same("p1"), same("r1"), same("v1")))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.headRuntime("p1", "r1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.NOT_FOUND));

        resp = controller.headRuntime("p2", "r1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testListRuntimeTree() {
        given(runtimeService.listRuntimeVersionView(anyString()))
                .willReturn(List.of(RuntimeViewVo.newBuilder().build()));

        var resp = controller.listRuntimeTree("1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody(), notNullValue());
        assertThat(resp.getBody().getData(), allOf(
                notNullValue(),
                is(iterableWithSize(1))
        ));
    }

    @Test
    public void testShareRuntimeVersion() {
        var resp = controller.shareRuntimeVersion("1", "1", "1", true);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testAddModelVersionTag() {
        doNothing().when(runtimeService).addRuntimeVersionTag("1", "2", "3", "tag1", false);

        var req = new RuntimeTagRequest();
        req.setTag("tag1");
        req.setForce(false);
        var resp = controller.addRuntimeVersionTag("1", "2", "3", req);

        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        verify(runtimeService).addRuntimeVersionTag("1", "2", "3", "tag1", false);
    }

    @Test
    public void testListModelVersionTags() {
        given(runtimeService.listRuntimeVersionTags("1", "2", "3"))
                .willReturn(List.of("tag1", "tag2"));

        var resp = controller.listRuntimeVersionTags("1", "2", "3");

        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody(), notNullValue());
        AssertionsForInterfaceTypes.assertThat(resp.getBody().getData()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    public void testDeleteModelVersionTag() {
        doNothing().when(runtimeService).deleteRuntimeVersionTag("1", "2", "3", "tag1");

        var resp = controller.deleteRuntimeVersionTag("1", "2", "3", "tag1");

        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        verify(runtimeService).deleteRuntimeVersionTag("1", "2", "3", "tag1");
    }
}
