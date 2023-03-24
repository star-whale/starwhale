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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.dataset.DatasetInfoVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetTagRequest;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.dataset.RevertDatasetRequest;
import ai.starwhale.mlops.api.protocol.dataset.upload.DatasetUploadRequest;
import ai.starwhale.mlops.api.protocol.upload.UploadPhase;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersionQuery;
import ai.starwhale.mlops.domain.dataset.objectstore.HashNamedDatasetObjectStoreFactory;
import ai.starwhale.mlops.domain.dataset.upload.DatasetUploader;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.objectstore.HashNamedObjectStore;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class DatasetControllerTest {

    private DatasetController controller;

    private DatasetService datasetService;

    private DatasetUploader datasetUploader;

    private HashNamedDatasetObjectStoreFactory hashNamedDatasetObjectStoreFactory;

    @BeforeEach
    public void setUp() {
        datasetService = mock(DatasetService.class);
        datasetUploader = mock(DatasetUploader.class);
        hashNamedDatasetObjectStoreFactory = mock(HashNamedDatasetObjectStoreFactory.class);

        controller = new DatasetController(datasetService, new IdConverter(), datasetUploader,
                hashNamedDatasetObjectStoreFactory);
    }

    @Test
    public void testRevertDatasetVersion() {
        given(datasetService.revertVersionTo(same("p1"), same("d1"), same("v1")))
                .willReturn(true);
        RevertDatasetRequest request = new RevertDatasetRequest();
        request.setVersionUrl("v1");
        var resp = controller.revertDatasetVersion("p1", "d1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.revertDatasetVersion("p2", "d1", request));
    }

    @Test
    public void testDeleteDataset() {
        given(datasetService.deleteDataset(argThat(argument -> Objects.equals(argument.getProjectUrl(), "p1")
                && Objects.equals(argument.getDatasetUrl(), "d1"))))
                .willReturn(true);

        var resp = controller.deleteDataset("p1", "d1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteDataset("p2", "d1"));
    }

    @Test
    public void testRecoverDataset() {
        given(datasetService.recoverDataset(same("p1"), same("d1")))
                .willReturn(true);
        var resp = controller.recoverDataset("p1", "d1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.recoverDataset("p2", "d1"));

    }

    @Test
    public void testGetDatasetInfo() {
        given(datasetService.getDatasetInfo(any(DatasetQuery.class)))
                .willAnswer((Answer<DatasetInfoVo>) invocation -> {
                    DatasetQuery query = invocation.getArgument(0);
                    if (Objects.equals(query.getProjectUrl(), "p1")) {
                        return DatasetInfoVo.builder()
                                .name(query.getDatasetUrl())
                                .versionName(query.getDatasetVersionUrl())
                                .build();
                    } else {
                        return null;
                    }
                });

        var resp = controller.getDatasetInfo("p1", "d1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("name", is("d1")),
                hasProperty("versionName", is("v1"))
        ));

        resp = controller.getDatasetInfo("p2", "d2", "v2");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), nullValue());
    }

    @Test
    public void testListDatasetVersion() {
        given(datasetService.listDatasetVersionHistory(any(DatasetVersionQuery.class), any(PageParams.class)))
                .willAnswer((Answer<PageInfo<DatasetVersionVo>>) invocation -> {
                    DatasetVersionQuery query = invocation.getArgument(0);
                    List<DatasetVersionVo> list = List.of(
                            DatasetVersionVo.builder()
                                    .name(query.getVersionName())
                                    .tag(query.getVersionTag())
                                    .build()
                    );
                    PageParams pageParams = invocation.getArgument(1);
                    PageInfo<DatasetVersionVo> pageInfo = new PageInfo<>(list);
                    pageInfo.setPageNum(pageParams.getPageNum());
                    pageInfo.setPageSize(pageParams.getPageSize());
                    return pageInfo;
                });
        var resp = controller.listDatasetVersion("p1", "d1", "v1", "tag1", 2, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("pageNum", is(2)),
                hasProperty("pageSize", is(5)),
                hasProperty("list", hasItem(allOf(
                        hasProperty("name", is("v1")),
                        hasProperty("tag", is("tag1"))
                )))
        ));
    }

    @Test
    public void testUploadDs() {
        given(datasetUploader.create(anyString(), anyString(), any(DatasetUploadRequest.class)))
                .willAnswer((Answer<Long>) invocation -> 1L);

        DatasetUploadRequest uploadRequest = new DatasetUploadRequest();
        uploadRequest.setUploadId(1L);
        uploadRequest.setPhase(UploadPhase.MANIFEST);

        MultipartFile file = new MockMultipartFile("dsFile", "originalName", null,
                "file_content".getBytes());
        var resp = controller.uploadDs(
                "p1", "d1", "v1", file, uploadRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("uploadId", is(1L))
        ));

        uploadRequest.setPhase(UploadPhase.BLOB);
        resp = controller.uploadDs("p1", "d1", "v1", file, uploadRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(),
                hasProperty("uploadId", is(1L)));

        uploadRequest.setPhase(UploadPhase.CANCEL);
        resp = controller.uploadDs("p1", "d1", "v1", file, uploadRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(),
                hasProperty("uploadId", is(1L)));

        uploadRequest.setPhase(UploadPhase.END);
        resp = controller.uploadDs("p1", "d1", "v1", file, uploadRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(),
                hasProperty("uploadId", is(1L)));
    }

    @Test
    public void testPullDs() {
        final AtomicBoolean called = new AtomicBoolean(false);
        doAnswer(invocation -> {
            called.set(true);
            return null;
        }).when(datasetUploader).pull(anyString(), anyString(), anyString(), anyString(), any());

        controller.pullDs("p1", "d1", "v1", "part", null);
        assertThat(called.get(), is(true));

        assertThrows(StarwhaleApiException.class,
                () -> controller.pullDs("p1", "", "v1", "part", null));

        assertThrows(StarwhaleApiException.class,
                () -> controller.pullDs("p1", "d1", "", "part", null));
    }

    @Test
    public void testPullLinkContent() throws IOException {
        StringBuilder str = new StringBuilder();
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
                        str.append(b);
                    }
                });
        given(datasetService.dataOf(any(), any(), any(), any(), any()))
                .willReturn(new byte[]{100});

        controller.pullUriContent("p1", "d1", "v1", 1L, 1L, response);
        assertThat(str.toString(), is("100"));
    }

    @Test
    public void testManageModelTag() {
        given(datasetService.manageVersionTag(same("p1"), same("d1"), same("v1"), argThat(
                argument -> Objects.equals(argument.getTags(), "tag1")))).willReturn(true);

        DatasetTagRequest reqeust = new DatasetTagRequest();
        reqeust.setTag("tag1");
        reqeust.setAction("add");
        var resp = controller.manageDatasetTag("p1", "d1", "v1", reqeust);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        reqeust.setAction("remove");
        resp = controller.manageDatasetTag("p1", "d1", "v1", reqeust);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        reqeust.setAction("set");
        resp = controller.manageDatasetTag("p1", "d1", "v1", reqeust);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.manageDatasetTag("p2", "d1", "v1", reqeust));

        reqeust.setAction("unknown");
        assertThrows(StarwhaleApiException.class,
                () -> controller.manageDatasetTag("p1", "d1", "v1", reqeust));

        reqeust.setAction("add");
        reqeust.setTag("no-tag");
        assertThrows(StarwhaleApiException.class,
                () -> controller.manageDatasetTag("p1", "d1", "v1", reqeust));
    }

    @Test
    public void testListDataset() {
        given(datasetService.findDatasetsByVersionIds(anyList()))
                .willReturn(List.of(DatasetVo.builder().id("1").build()));
        given(datasetService.listDataset(any(DatasetQuery.class), any(PageParams.class)))
                .willReturn(PageInfo.of(List.of(
                        DatasetVo.builder().id("1").build(),
                        DatasetVo.builder().id("2").build()
                )));

        var resp = controller.listDataset("", "3", "", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(1))
        ));

        resp = controller.listDataset("project1", "", "", "", 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                hasProperty("list", iterableWithSize(2))
        ));
    }

    @Test
    public void testHeadDataset() {
        given(datasetService.query(same("p1"), same("d1"), same("v1")))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.headDataset("p1", "d1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.NOT_FOUND));

        resp = controller.headDataset("p2", "d1", "v1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testSignLinks() {
        String pj = "pj";
        String ds = "ds";
        String uri = "uri";
        String signUrl = "sign-url";
        when(datasetService.signLinks(pj, "ds", Set.of(uri), 100L)).thenReturn(Map.of(uri, signUrl));
        Assertions.assertEquals(Map.of(uri, signUrl),
                controller.signLinks(pj, ds, Set.of(uri), 100L).getBody().getData());
    }

    @Test
    public void testHeadHashedBlob() throws IOException {
        HashNamedObjectStore hashNamedObjectStore = mock(HashNamedObjectStore.class);
        when(hashNamedDatasetObjectStoreFactory.of("p", "d")).thenReturn(hashNamedObjectStore);
        when(hashNamedObjectStore.head("h1")).thenReturn(new StorageObjectInfo(true, null, null));
        when(hashNamedObjectStore.head("h2")).thenReturn(new StorageObjectInfo(false, null, null));
        when(hashNamedObjectStore.head("h3")).thenThrow(IOException.class);
        Assertions.assertTrue(controller.headHashedBlob("p", "d", "h1").getStatusCode().is2xxSuccessful());
        Assertions.assertTrue(controller.headHashedBlob("p", "d", "h2").getStatusCode().is4xxClientError());
        Assertions.assertThrows(SwProcessException.class,
                () -> controller.headHashedBlob("p", "d", "h3").getStatusCode().is4xxClientError());

    }
}
