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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.api.protocol.swmp.ClientSwmpRequest;
import ai.starwhale.mlops.api.protocol.swmp.RevertSwmpVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageInfoVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVersionVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVo;
import ai.starwhale.mlops.api.protocol.swmp.SwmpTagRequest;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.swmp.SwModelPackageService;
import ai.starwhale.mlops.domain.swmp.bo.SwmpQuery;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersionQuery;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class SwModelPackageControllerTest {

    private SwModelPackageController controller;
    private SwModelPackageService swmpService;

    @BeforeEach
    public void setUp() {
        swmpService = mock(SwModelPackageService.class);
        controller = new SwModelPackageController(swmpService, new IdConvertor());
    }

    @Test
    public void testListModel() {
        given(swmpService.findModelByVersionId(anyList()))
                .willReturn(List.of(SwModelPackageVo.builder().id("1").build()));
        given(swmpService.listSwmp(any(SwmpQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        SwModelPackageVo.builder().id("1").build(),
                        SwModelPackageVo.builder().id("2").build()
                )));

        var resp = controller.listModel("", "3", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(1))
        ));

        resp = controller.listModel("project1", "", "model1", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(2))
        ));
    }

    @Test
    public void testRevertModelVersion() {
        given(swmpService.revertVersionTo(same("1"), same("2"), same("3")))
                .willReturn(true);

        RevertSwmpVersionRequest request = new RevertSwmpVersionRequest();
        request.setVersionUrl("3");
        var resp = controller.revertModelVersion("1", "2", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setVersionUrl("4");
        assertThrows(StarwhaleApiException.class,
                () -> controller.revertModelVersion("1", "2", request));
    }

    @Test
    public void testDeleteModel() {
        given(swmpService.deleteSwmp(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getSwmpUrl(), "m1")))).willReturn(true);
        var resp = controller.deleteModel("p1", "m1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteModel("p2", "m1"));

    }

    @Test
    public void testRecoverModel() {
        given(swmpService.getSwmpInfo(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getSwmpUrl(), "m1")
                && Objects.equals(argument.getSwmpVersionUrl(), "v1")))
        ).willReturn(SwModelPackageInfoVo.builder().id("1").build());
        var resp = controller.getModelInfo("p1", "m1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("id", is("1"))
        ));
        resp = controller.getModelInfo("p2", "m1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), nullValue());
    }

    @Test
    public void testListModelVersion() {
        given(swmpService.listSwmpVersionHistory(any(SwmpVersionQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        SwModelPackageVersionVo.builder().id("1").build(),
                        SwModelPackageVersionVo.builder().id("2").build()
                )));

        var resp = controller.listModelVersion("p1", "m1", "v1", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(2))
        ));
    }

    @Test
    public void testModifyModel() {
        given(swmpService.modifySwmpVersion(same("p1"), same("m1"), same("v1"), any()))
                .willReturn(true);

        var resp = controller.modifyModel("p1", "m1", "v1", new SwmpTagRequest());
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThrows(StarwhaleApiException.class,
                () -> controller.modifyModel("p2", "m1", "v1", new SwmpTagRequest()));
    }

    @Test
    public void testManageModelTag() {
        given(swmpService.manageVersionTag(same("p1"), same("m1"), same("v1"), argThat(
                argument -> Objects.equals(argument.getTags(), "tag1")))).willReturn(true);

        SwmpTagRequest reqeust = new SwmpTagRequest();
        reqeust.setTag("tag1");
        reqeust.setAction("add");
        var resp = controller.manageModelTag("p1", "m1", "v1", reqeust);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        reqeust.setAction("remove");
        resp = controller.manageModelTag("p1", "m1", "v1", reqeust);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        reqeust.setAction("set");
        resp = controller.manageModelTag("p1", "m1", "v1", reqeust);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.manageModelTag("p2", "m1", "v1", reqeust));

        reqeust.setAction("unknown");
        assertThrows(StarwhaleApiException.class,
                () -> controller.manageModelTag("p1", "m1", "v1", reqeust));

        reqeust.setAction("add");
        reqeust.setTag("no-tag");
        assertThrows(StarwhaleApiException.class,
                () -> controller.manageModelTag("p1", "m1", "v1", reqeust));
    }

    @Test
    public void testUpload() {
        var resp = controller.upload("p1", "m1", "v1", null, new ClientSwmpRequest());
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testPull() {
        controller.pull("p1", "m1", "v1", null);
    }

    @Test
    public void testHeadModel() {
        given(swmpService.query(same("p1"), same("m1"), same("v1")))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.headModel("p1", "m1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.NOT_FOUND));

        resp = controller.headModel("p2", "m1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }
}
