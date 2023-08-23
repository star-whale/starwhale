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

import static ai.starwhale.mlops.configuration.FeaturesProperties.FEATURE_JOB_DEV;
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
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.job.ExecRequest;
import ai.starwhale.mlops.api.protocol.job.ExecResponse;
import ai.starwhale.mlops.api.protocol.job.JobModifyPinRequest;
import ai.starwhale.mlops.api.protocol.job.JobModifyRequest;
import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.job.ModelServingRequest;
import ai.starwhale.mlops.api.protocol.job.ModelServingStatusVo;
import ai.starwhale.mlops.api.protocol.job.ModelServingVo;
import ai.starwhale.mlops.api.protocol.job.RuntimeSuggestionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.dag.DagQuerier;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.JobServiceForWeb;
import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.job.RuntimeSuggestionService;
import ai.starwhale.mlops.domain.task.TaskService;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceEventHolder;
import com.github.pagehelper.Page;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class JobControllerTest {

    private JobController controller;

    private JobServiceForWeb jobServiceForWeb;

    private TaskService taskService;

    private ModelServingService modelServingService;

    private DagQuerier dagQuerier;

    private RuntimeSuggestionService runtimeSuggestionService;

    private FeaturesProperties featuresProperties;

    @BeforeEach
    public void setUp() {
        jobServiceForWeb = mock(JobServiceForWeb.class);
        taskService = mock(TaskService.class);
        modelServingService = mock(ModelServingService.class);
        dagQuerier = mock(DagQuerier.class);
        runtimeSuggestionService = mock(RuntimeSuggestionService.class);
        featuresProperties = new FeaturesProperties();
        controller = new JobController(
                jobServiceForWeb,
                taskService,
                modelServingService,
                runtimeSuggestionService,
                new IdConverter(),
                dagQuerier,
                featuresProperties
        );
    }

    @Test
    public void testListJobs() {
        given(jobServiceForWeb.listJobs(same("p1"), same(1L), any(PageParams.class)))
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
        given(jobServiceForWeb.findJob(same("p1"), same("j1")))
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
        given(jobServiceForWeb.createJob(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), any(), eq(DevWay.VS_CODE), eq(false), anyString(),
                any())).willReturn(1L);
        JobRequest jobRequest = new JobRequest();
        jobRequest.setHandler("eval");
        jobRequest.setComment("");
        jobRequest.setModelVersionUrl("");
        jobRequest.setDatasetVersionUrls("");
        jobRequest.setRuntimeVersionUrl("");
        jobRequest.setResourcePool("");
        jobRequest.setDevWay(DevWay.VS_CODE);
        jobRequest.setDevPassword("");
        var resp = controller.createJob("p1", jobRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is("1")
        ));

        // debug is disabled
        featuresProperties.setDisabled(List.of(FEATURE_JOB_DEV));
        jobRequest.setDevMode(true);
        assertThrows(StarwhaleApiException.class, () -> controller.createJob("p1", jobRequest));
    }

    private String invoked = "";

    @Test
    public void testAction() {
        doAnswer(invocation -> invoked = "cancel_" + invocation.getArgument(0))
                .when(jobServiceForWeb).cancelJob(anyString());
        doAnswer(invocation -> invoked = "pause_" + invocation.getArgument(0))
                .when(jobServiceForWeb).pauseJob(anyString());
        doAnswer(invocation -> invoked = "resume_" + invocation.getArgument(0))
                .when(jobServiceForWeb).resumeJob(anyString());

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
        given(jobServiceForWeb.getJobResult(anyString(), anyString()))
                .willAnswer(invocation -> "result_" + invocation.getArgument(0)
                        + "_" + invocation.getArgument(1));
        var resp = controller.getJobResult("project1", "job1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is("result_project1_job1"));
    }

    @Test
    public void testModifyJobComment() {
        given(jobServiceForWeb.updateJobComment(same("p1"), same("j1"), same("comment1")))
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
        given(jobServiceForWeb.removeJob(same("p1"), same("j1")))
                .willReturn(true);

        var resp = controller.removeJob("p1", "j1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.removeJob("p1", "j2"));
    }

    @Test
    public void testRecoverJob() {
        given(jobServiceForWeb.recoverJob(same("p1"), same("j1")))
                .willReturn(true);

        var resp = controller.recoverJob("p1", "j1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.recoverJob("p1", "j2"));
    }

    @Test
    public void testCreateModelServing() {
        var vo = ModelServingVo.builder().id("8").baseUri("/gateway/model-serving/8").build();
        given(modelServingService.create("foo", "bar", "baz", "default", null)).willReturn(vo);
        var req = new ModelServingRequest();
        req.setModelVersionUrl("bar");
        req.setRuntimeVersionUrl("baz");
        req.setResourcePool("default");
        var resp = controller.createModelServing("foo", req);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody().getData(), is(vo));
    }

    @Test
    public void testGetModelServingStatus() {
        var event = ResourceEventHolder.Event.builder().name("foo").build();
        var vo = ModelServingStatusVo.builder().progress(100).events(List.of(event)).build();
        given(modelServingService.getStatus(2L)).willReturn(vo);
        var resp = controller.getModelServingStatus(1L, 2L);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody().getData(), is(vo));
    }

    @Test
    public void testRuntimeSuggestion() {
        var rt = RuntimeVersionVo.builder().id("1").image("foo").build();
        var vo = RuntimeSuggestionVo.builder().runtimes(List.of(rt)).build();
        given(runtimeSuggestionService.getSuggestions(2L, null)).willReturn(List.of(rt));
        var resp = controller.getRuntimeSuggestion(2L, null);
        assertThat(resp.getBody().getData(), is(vo));
    }

    @Test
    public void testModelServingWithFeatureDisabled() {
        var disabled = List.of("online-eval");
        featuresProperties.setDisabled(disabled);
        assertThrows(StarwhaleApiException.class,
                () -> controller.createModelServing("foo", new ModelServingRequest()));
        assertThrows(StarwhaleApiException.class,
                () -> controller.getModelServingStatus(1L, 2L));
    }

    @Test
    public void testJobPauseDisabled() {
        var disabled = List.of("job-pause");
        featuresProperties.setDisabled(disabled);
        var controller = new JobController(
                jobServiceForWeb,
                taskService,
                modelServingService,
                runtimeSuggestionService,
                new IdConverter(),
                dagQuerier,
                featuresProperties
        );
        assertThrows(StarwhaleApiException.class,
                () -> controller.action("", "job1", "pause"));

        // resume is not disabled
        controller.action("", "job1", "resume");
    }

    @Test
    public void testJobResumeDisabled() {
        var disabled = List.of("job-resume");
        featuresProperties.setDisabled(disabled);
        var controller = new JobController(
                jobServiceForWeb,
                taskService,
                modelServingService,
                runtimeSuggestionService,
                new IdConverter(),
                dagQuerier,
                featuresProperties
        );
        assertThrows(StarwhaleApiException.class,
                () -> controller.action("", "job1", "resume"));

        // pause is not disabled
        controller.action("", "job1", "pause");
    }

    @Test
    public void testJobExec() {
        when(jobServiceForWeb.exec(anyString(), anyString(), anyString(), any()))
                .thenReturn(ExecResponse.builder().stdout("foo").stderr("bar").build());
        var resp = controller.exec("p1", "j1", "t1", new ExecRequest());
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(resp.getBody().getData().getStdout(), is("foo"));
        assertThat(resp.getBody().getData().getStderr(), is("bar"));
    }


    @Test
    public void testModifyJobPinStatus() {
        given(jobServiceForWeb.updateJobPinStatus(same("p1"), same("j1"), same(true)))
                .willReturn(true);
        JobModifyPinRequest request = new JobModifyPinRequest();
        request.setPinned(true);
        var resp = controller.modifyJobPinStatus("p1", "j1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.modifyJobPinStatus("p1", "j2", request));
    }
}
