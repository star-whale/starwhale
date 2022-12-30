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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.job.JobModifyRequest;
import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.job.ModelServingRequest;
import ai.starwhale.mlops.api.protocol.job.ModelServingVo;
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.dag.DagQuerier;
import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.task.TaskService;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.Page;
import io.kubernetes.client.openapi.ApiException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class JobControllerTest {

    private JobController controller;

    private JobService jobService;

    private TaskService taskService;

    private ModelServingService modelServingService;

    private DagQuerier dagQuerier;

    @BeforeEach
    public void setUp() {
        jobService = mock(JobService.class);
        taskService = mock(TaskService.class);
        modelServingService = mock(ModelServingService.class);
        dagQuerier = mock(DagQuerier.class);
        controller = new JobController(
                jobService,
                taskService,
                modelServingService,
                new IdConverter(),
                dagQuerier
        );
    }

    @Test
    public void testListJobs() {
        given(jobService.listJobs(same("p1"), same(1L), any(PageParams.class)))
                .willAnswer(invocation -> {
                    PageParams pageParams = invocation.getArgument(2);
                    try (Page<JobVo> page = new Page<>(pageParams.getPageNum(), pageParams.getPageSize())) {
                        page.add(JobVo.builder().build());
                        return page.toPageInfo();
                    }
                });
        var resp = controller.listJobs("p1", "1", 3, 10);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(3))),
                is(hasProperty("pageSize", is(10))),
                is(hasProperty("list", isA(List.class)))
        ));
    }

    @Test
    public void testFindJob() {
        given(jobService.findJob(same("p1"), same("j1")))
                .willReturn(JobVo.builder().id("j1").build());
        var resp = controller.findJob("p1", "j1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("id", is("j1")))
        ));
    }

    @Test
    public void testListTasks() {
        given(taskService.listTasks(same("j1"), any(PageParams.class)))
                .willAnswer(invocation -> {
                    PageParams pageParams = invocation.getArgument(1);
                    try (Page<TaskVo> page = new Page<>(pageParams.getPageNum(), pageParams.getPageSize())) {
                        page.add(TaskVo.builder().build());
                        return page.toPageInfo();
                    }
                });
        var resp = controller.listTasks("p1", "j1", 3, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(3))),
                is(hasProperty("pageSize", is(5))),
                is(hasProperty("list", isA(List.class)))
        ));
    }

    @Test
    public void testCreatJob() {
        given(jobService.createJob(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(1L);
        JobRequest jobRequest = new JobRequest();
        jobRequest.setComment("");
        jobRequest.setModelVersionUrl("");
        jobRequest.setDatasetVersionUrls("");
        jobRequest.setRuntimeVersionUrl("");
        jobRequest.setResourcePool("");
        var resp = controller.createJob("p1", jobRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is("1")
        ));

    }

    private String invoked = "";

    @Test
    public void testAction() {
        doAnswer(invocation -> invoked = "cancel_" + invocation.getArgument(0))
                .when(jobService).cancelJob(anyString());
        doAnswer(invocation -> invoked = "pause_" + invocation.getArgument(0))
                .when(jobService).pauseJob(anyString());
        doAnswer(invocation -> invoked = "resume_" + invocation.getArgument(0))
                .when(jobService).resumeJob(anyString());

        controller.action("", "job1", "cancel");
        assertThat(invoked, is("cancel_job1"));

        controller.action("", "job2", "pause");
        assertThat(invoked, is("pause_job2"));

        controller.action("", "job3", "resume");
        assertThat(invoked, is("resume_job3"));

        assertThrows(StarwhaleApiException.class,
                () -> controller.action("", "job1", null));

        assertThrows(StarwhaleApiException.class,
                () -> controller.action("", "job1", "a"));
    }

    @Test
    public void testGetJobResult() {
        given(jobService.getJobResult(anyString(), anyString()))
                .willAnswer(invocation -> "result_" + invocation.getArgument(0)
                        + "_" + invocation.getArgument(1));
        var resp = controller.getJobResult("project1", "job1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is("result_project1_job1"));
    }

    @Test
    public void testModifyJobComment() {
        given(jobService.updateJobComment(same("p1"), same("j1"), same("comment1")))
                .willReturn(true);
        JobModifyRequest request = new JobModifyRequest();
        request.setComment("comment1");
        var resp = controller.modifyJobComment("p1", "j1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.modifyJobComment("p1", "j2", request));
    }

    @Test
    public void testRemoveJob() {
        given(jobService.removeJob(same("p1"), same("j1")))
                .willReturn(true);

        var resp = controller.removeJob("p1", "j1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.removeJob("p1", "j2"));
    }

    @Test
    public void testRecoverJob() {
        given(jobService.recoverJob(same("p1"), same("j1")))
                .willReturn(true);

        var resp = controller.recoverJob("p1", "j1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.recoverJob("p1", "j2"));
    }

    @Test
    public void testCreateModelServing() {
        given(modelServingService.create("foo", "bar", "baz", "default", 7L)).willReturn(8L);
        var req = new ModelServingRequest();
        req.setModelVersionUrl("bar");
        req.setRuntimeVersionUrl("baz");
        req.setResourcePool("default");
        req.setTtlInSeconds(7L);
        var resp = controller.createModelServing("foo", req);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody().getData(), is("8"));
    }

    @Test
    public void testListModelServing() {
        var serving = ModelServingVo.builder()
                .modelVersion("foo")
                .runtimeVersion("bar")
                .resourcePool("default")
                .build();
        var ret = PageUtil.toPageInfo(List.of(serving), i -> i);
        given(modelServingService.listServing(eq("foo"), any(PageParams.class))).willReturn(ret);

        var resp = controller.listServing("foo", 1, 10);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody().getData(), is(ret));
    }

    @Test
    public void testRemoveModelServing() throws ApiException {
        var resp = controller.removeServing("foo", "bar");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        verify(modelServingService).remove("foo", "bar");
    }
}
