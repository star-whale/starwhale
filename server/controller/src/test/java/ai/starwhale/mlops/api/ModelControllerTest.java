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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelTagRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUpdateRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUploadRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelViewVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.model.RevertModelVersionRequest;
import ai.starwhale.mlops.api.protocol.storage.FileDesc;
import ai.starwhale.mlops.api.protocol.upload.UploadPhase;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ModelControllerTest {

    private ModelController controller;
    private ModelService modelService;

    @BeforeEach
    public void setUp() {
        modelService = mock(ModelService.class);
        controller = new ModelController(modelService, new IdConverter());
    }

    @Test
    public void testListModel() {
        given(modelService.findModelByVersionId(anyList()))
                .willReturn(List.of(ModelVo.builder().id("1").build()));
        given(modelService.listModel(any(ModelQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        ModelVo.builder().id("1").build(),
                        ModelVo.builder().id("2").build()
                )));

        var resp = controller.listModel("", "3", "", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(1))
        ));

        resp = controller.listModel("project1", "", "model1", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(2))
        ));
    }

    @Test
    public void testModelDiff() {
        given(modelService.getModelDiff(any(), any(), any(), any()))
                .willReturn(Map.of("baseVersion", List.of(), "compareVersion", List.of()));
        var res = controller.getModelDiff("p", "m", "b1", "c1");
        assertThat(res.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testRevertModelVersion() {
        given(modelService.revertVersionTo(same("1"), same("2"), same("3")))
                .willReturn(true);

        RevertModelVersionRequest request = new RevertModelVersionRequest();
        request.setVersionUrl("3");
        var resp = controller.revertModelVersion("1", "2", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setVersionUrl("4");
        assertThrows(StarwhaleApiException.class,
                () -> controller.revertModelVersion("1", "2", request));
    }

    @Test
    public void testDeleteModel() {
        given(modelService.deleteModel(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getModelUrl(), "m1")))).willReturn(true);
        var resp = controller.deleteModel("p1", "m1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteModel("p2", "m1"));

    }

    @Test
    public void testRecoverModel() {
        given(modelService.getModelInfo(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getModelUrl(), "m1")
                && Objects.equals(argument.getModelVersionUrl(), "v1")))
        ).willReturn(ModelInfoVo.builder().id("1").build());
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
        given(modelService.listModelVersionHistory(any(ModelVersionQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        ModelVersionVo.builder().id("1").build(),
                        ModelVersionVo.builder().id("2").build()
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
        given(modelService.modifyModelVersion(same("p1"), same("m1"), same("v1"), any()))
                .willReturn(true);

        var resp = controller.modifyModel("p1", "m1", "v1", new ModelUpdateRequest());
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThrows(StarwhaleApiException.class,
                () -> controller.modifyModel("p2", "m1", "v1", new ModelUpdateRequest()));
    }

    @Test
    public void testManageModelTag() {
        given(modelService.manageVersionTag(same("p1"), same("m1"), same("v1"), argThat(
                argument -> Objects.equals(argument.getTags(), "tag1")))).willReturn(true);

        ModelTagRequest reqeust = new ModelTagRequest();
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
        var request = new ModelUploadRequest();
        request.setPhase(UploadPhase.MANIFEST);
        request.setUploadId(1L);
        request.setDesc(FileDesc.MANIFEST);
        request.setSignature("");
        var resp = controller.upload(
                "p1", "m1", "v1", null, request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setPhase(UploadPhase.BLOB);
        request.setDesc(FileDesc.SRC_TAR);
        resp = controller.upload(
                "p1", "m1", "v1", null, request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setDesc(FileDesc.MODEL);
        resp = controller.upload(
                "p1", "m1", "v1", null, request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setPhase(UploadPhase.CANCEL);
        assertThrows(StarwhaleApiException.class, () -> controller.upload(
                "p1", "m1", "v1", null, request));

        request.setPhase(UploadPhase.END);
        controller.upload(
                "p1", "m1", "v1", null, request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testPull() {
        controller.pull(
                FileDesc.MANIFEST, "manifest.yaml", "manifest.yaml", "", "p1", "m1", "v1", null);
        controller.pull(
                FileDesc.SRC_TAR, "model.yaml", "src/model.yaml", "", "p1", "m1", "v1", null);
        controller.pull(
                FileDesc.MODEL, "mnist.pth", "src/models/mnist.pth", "qwer", "p1", "m1", "v1", null);
    }

    @Test
    public void testHeadModel() {
        given(modelService.query(same("p1"), same("m1"), same("v1")))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.headModel("p1", "m1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.NOT_FOUND));

        resp = controller.headModel("p2", "m1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testListModelTree() {
        given(modelService.listModelVersionView(anyString()))
                .willReturn(List.of(ModelViewVo.builder().build()));

        var resp = controller.listModelTree("1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody(), notNullValue());
        assertThat(resp.getBody().getData(), allOf(
                notNullValue(),
                is(iterableWithSize(1))
        ));
    }

    @Test
    public void testShareModelVersion() {
        var resp = controller.shareModelVersion("1", "1", "1", true);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }
}
