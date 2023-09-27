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

package ai.starwhale.mlops.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.job.JobServiceForWeb;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.report.ReportService;
import ai.starwhale.mlops.domain.report.bo.QueryParam;
import ai.starwhale.mlops.domain.report.mapper.ReportMapper;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.RuntimeTestConstants;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.task.TaskService;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.impl.docker.ContainerRunMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinderSimpleImpl;
import ai.starwhale.mlops.schedule.impl.docker.log.RunLogCollectorFactoryDocker;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceEventHolder;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@MybatisTest
@ComponentScan(
        basePackages = {
                "ai.starwhale.mlops.common",
                "ai.starwhale.mlops.domain",
                "ai.starwhale.mlops.datastore",
                "ai.starwhale.mlops.schedule.reporting",
                "ai.starwhale.mlops.schedule.log",
                "ai.starwhale.mlops.schedule.impl.docker",
                "ai.starwhale.mlops.schedule.impl.container",
                "ai.starwhale.mlops.resulting",
                "ai.starwhale.mlops.configuration.security"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {ModelServingService.class}),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test.*")
        }
)
@ImportAutoConfiguration(PageHelperAutoConfiguration.class)
@Import({K8sJobTemplate.class, ResourceEventHolder.class, SimpleMeterRegistry.class,
        TaskContainerSpecificationFinder.class,
        RunLogCollectorFactoryDocker.class,
        DockerClientFinderSimpleImpl.class,
        ContainerRunMapper.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PageTest extends MySqlContainerHolder {

    @Autowired
    private JobServiceForWeb jobServiceForWeb;
    @Autowired
    private JobMapper jobMapper;
    @Autowired
    private StepMapper stepMapper;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelVersionMapper modelVersionMapper;

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RuntimeMapper runtimeMapper;
    @Autowired
    private RuntimeVersionMapper runtimeVersionMapper;

    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;

    String userName = "user-test";
    String projectName = "project-test";

    @BeforeEach
    public void before() {
        UserEntity userEntity = UserEntity.builder()
                .userName(userName)
                .userPwd("123456")
                .userPwdSalt("123456")
                .userEnabled(1)
                .build();
        userMapper.insert(userEntity);

        ProjectEntity entity = ProjectEntity.builder()
                .projectName(projectName)
                .ownerId(userEntity.getId())
                .privacy(Project.Privacy.PUBLIC.getValue())
                .isDefault(1)
                .build();
        projectMapper.insert(entity);
    }

    @Test
    public void testReportList() {
        var userId = userMapper.findByName(userName).getId();
        var projectId = projectMapper.findByNameForUpdateAndOwner(projectName, userId).getId();
        for (int i = 0; i < 19; i++) {
            var res = reportMapper.insert(
                    ReportEntity.builder()
                            .uuid(UUID.randomUUID().toString())
                            .title(String.format("report-%d", i))
                            .content(String.format("content-%d", i))
                            .projectId(projectId)
                            .ownerId(userId)
                            .build());
            assertTrue(res > 0);
        }

        // no.1
        var page = reportService.listReport(
                QueryParam.builder().projectUrl(projectName).build(),
                PageParams.builder().pageNum(1).pageSize(10).build()
        );
        assertEquals(10, page.getSize());
        assertEquals(19, page.getTotal());

        // no.2
        page = reportService.listReport(
                QueryParam.builder().projectUrl(projectName).build(),
                PageParams.builder().pageNum(2).pageSize(10).build()
        );
        assertEquals(9, page.getSize());
        assertEquals(19, page.getTotal());
    }

    @Test
    public void testModelList() {
        var userId = userMapper.findByName(userName).getId();
        var projectId = projectMapper.findByNameForUpdateAndOwner(projectName, userId).getId();
        for (int i = 0; i < 19; i++) {
            var res = modelMapper.insert(
                    ModelEntity.builder()
                            .modelName(String.format("model-%d", i))
                            .projectId(projectId)
                            .ownerId(userId)
                            .build());
            assertTrue(res > 0);
        }

        // no.1
        var page = modelService.listModel(
                ModelQuery.builder().projectUrl(projectName).owner(userName).build(),
                PageParams.builder().pageNum(1).pageSize(10).build()
        );
        assertEquals(10, page.getSize());
        assertEquals(19, page.getTotal());

        // no.2
        page = modelService.listModel(
                ModelQuery.builder().projectUrl(projectName).owner(userName).build(),
                PageParams.builder().pageNum(2).pageSize(10).build()
        );
        assertEquals(9, page.getSize());
        assertEquals(19, page.getTotal());
    }

    @Test
    public void testRuntimeList() {
        var userId = userMapper.findByName(userName).getId();
        var projectId = projectMapper.findByNameForUpdateAndOwner(projectName, userId).getId();
        for (int i = 0; i < 19; i++) {
            var res = runtimeMapper.insert(
                    RuntimeEntity.builder()
                            .runtimeName(String.format("rt-%d", i))
                            .projectId(projectId)
                            .ownerId(userId)
                            .build());
            assertTrue(res > 0);
        }

        // no.1
        var page = runtimeService.listRuntime(
                RuntimeQuery.builder().projectUrl(projectName).owner(userName).build(),
                PageParams.builder().pageNum(1).pageSize(10).build()
        );
        assertEquals(10, page.getSize());
        assertEquals(19, page.getTotal());

        // no.2
        page = runtimeService.listRuntime(
                RuntimeQuery.builder().projectUrl(projectName).owner(userName).build(),
                PageParams.builder().pageNum(2).pageSize(10).build()
        );
        assertEquals(9, page.getSize());
        assertEquals(19, page.getTotal());
    }

    @Test
    public void testDatasetList() {
        var userId = userMapper.findByName(userName).getId();
        var projectId = projectMapper.findByNameForUpdateAndOwner(projectName, userId).getId();
        for (int i = 0; i < 19; i++) {
            var res = datasetMapper.insert(
                    DatasetEntity.builder()
                            .datasetName(String.format("ds-%d", i))
                            .projectId(projectId)
                            .ownerId(userId)
                            .build());
            assertTrue(res > 0);
        }

        // no.1
        var page = datasetService.listDataset(
                DatasetQuery.builder().projectUrl(projectName).owner(userName).build(),
                PageParams.builder().pageNum(1).pageSize(10).build()
        );
        assertEquals(10, page.getSize());
        assertEquals(19, page.getTotal());

        // no.2
        page = datasetService.listDataset(
                DatasetQuery.builder().projectUrl(projectName).owner(userName).build(),
                PageParams.builder().pageNum(2).pageSize(10).build()
        );
        assertEquals(9, page.getSize());
        assertEquals(19, page.getTotal());
    }

    @Test
    public void testJobList() {
        var userId = userMapper.findByName(userName).getId();
        var projectId = projectMapper.findByNameForUpdateAndOwner(projectName, userId).getId();

        // prepare data
        var model = ModelEntity.builder()
                .modelName(String.format("model-%d", 0))
                .projectId(projectId)
                .ownerId(userId)
                .build();
        assertTrue(modelMapper.insert(model) > 0);
        var modelVersion = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("model-version-1")
                .ownerId(userId)
                .jobs("mnist.evaluator:MNISTInference.cmp:\n"
                        + "- cls_name: ''\n"
                        + "  concurrency: 1\n"
                        + "  needs: []\n"
                        + "  resources: []\n"
                        + "  name: mnist.evaluator:MNISTInference.ppl\n"
                        + "  replicas: 1\n"
                        + "- cls_name: ''\n"
                        + "  concurrency: 1\n"
                        + "  needs:\n"
                        + "  - mnist.evaluator:MNISTInference.ppl\n"
                        + "  resources:\n"
                        + "  - type: cpu \n"
                        + "    request: 0.1\n"
                        + "    limit: 0.1\n"
                        + "  - type: nvidia.com/gpu \n"
                        + "    request: 1\n"
                        + "    limit: 1\n"
                        + "  - type: memory \n"
                        + "    request: 1\n"
                        + "    limit: 1\n"
                        + "  name: mnist.evaluator:MNISTInference.cmp\n"
                        + "  replicas: 1\n")
                .build();
        assertTrue(modelVersionMapper.insert(modelVersion) > 0);

        var runtime = RuntimeEntity.builder()
                .runtimeName(String.format("rt-%d", 0))
                .projectId(projectId)
                .ownerId(userId)
                .build();
        assertTrue(runtimeMapper.insert(runtime) > 0);
        var runtimeVersion = RuntimeVersionEntity.builder()
                .runtimeId(runtime.getId())
                .versionName("rt-version-1")
                .ownerId(userId)
                .versionTag("v1")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .storagePath("path")
                .build();
        assertTrue(runtimeVersionMapper.insert(runtimeVersion) > 0);

        for (int i = 0; i < 19; i++) {
            var res = jobMapper.addJob(
                    JobEntity.builder()
                            .name(String.format("job-%d", i))
                            .jobUuid(String.format("uuid-%d", i))
                            .modelVersionId(modelVersion.getId())
                            .runtimeVersionId(runtimeVersion.getId())
                            .type(JobType.EVALUATION)
                            .jobStatus(JobStatus.CREATED)
                            .resultOutputPath("path")
                            .projectId(projectId)
                            .ownerId(userId)
                            .isDeleted(0)
                            .build());
            assertTrue(res > 0);
        }

        // no.1
        var page = jobServiceForWeb.listJobs(projectName, null,
                PageParams.builder().pageNum(1).pageSize(10).build());
        assertEquals(10, page.getSize());
        assertEquals(19, page.getTotal());

        // no.2
        page = jobServiceForWeb.listJobs(projectName, null,
                PageParams.builder().pageNum(2).pageSize(10).build());
        assertEquals(9, page.getSize());
        assertEquals(19, page.getTotal());
    }

    @Test
    public void testTaskList() {
        var userId = userMapper.findByName(userName).getId();
        var projectId = projectMapper.findByNameForUpdateAndOwner(projectName, userId).getId();

        // prepare data
        var model = ModelEntity.builder()
                .modelName(String.format("model-%d", 0))
                .projectId(projectId)
                .ownerId(userId)
                .build();
        assertTrue(modelMapper.insert(model) > 0);
        var modelVersion = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("model-version-1")
                .ownerId(userId)
                .jobs("mnist.evaluator:MNISTInference.cmp:\n"
                        + "- cls_name: ''\n"
                        + "  concurrency: 1\n"
                        + "  needs: []\n"
                        + "  resources: []\n"
                        + "  name: mnist.evaluator:MNISTInference.ppl\n"
                        + "  replicas: 1\n"
                        + "- cls_name: ''\n"
                        + "  concurrency: 1\n"
                        + "  needs:\n"
                        + "  - mnist.evaluator:MNISTInference.ppl\n"
                        + "  resources:\n"
                        + "  - type: cpu \n"
                        + "    request: 0.1\n"
                        + "    limit: 0.1\n"
                        + "  - type: nvidia.com/gpu \n"
                        + "    request: 1\n"
                        + "    limit: 1\n"
                        + "  - type: memory \n"
                        + "    request: 1\n"
                        + "    limit: 1\n"
                        + "  name: mnist.evaluator:MNISTInference.cmp\n"
                        + "  replicas: 1\n")
                .build();
        assertTrue(modelVersionMapper.insert(modelVersion) > 0);
        var job = JobEntity.builder()
                .name("job-1")
                .jobUuid("uuid-1")
                .modelVersionId(modelVersion.getId())
                .runtimeVersionId(1L)
                .type(JobType.EVALUATION)
                .jobStatus(JobStatus.CREATED)
                .resultOutputPath("path")
                .projectId(projectId)
                .ownerId(userId)
                .isDeleted(0)
                .build();
        assertTrue(jobMapper.addJob(job) > 0);

        var step = StepEntity.builder()
                .jobId(job.getId())
                .name("step-1")
                .status(StepStatus.CREATED)
                .uuid("uuid-step-1")
                .concurrency(1)
                .taskNum(1)
                .poolInfo("")
                .build();
        assertTrue(stepMapper.save(step) > 0);

        for (int i = 0; i < 19; i++) {
            var res = taskMapper.addTask(
                    TaskEntity.builder()
                            .stepId(step.getId())
                            .taskUuid(String.format("uuid-%d", i))
                            .taskStatus(TaskStatus.CREATED)
                            .taskRequest("request")
                            .build());
            assertTrue(res > 0);
        }

        // no.1
        var page = taskService.listTasks(job.getJobUuid(),
                PageParams.builder().pageNum(1).pageSize(10).build());
        assertEquals(10, page.getSize());
        assertEquals(19, page.getTotal());

        // no.2
        page = taskService.listTasks(job.getJobUuid(),
                PageParams.builder().pageNum(2).pageSize(10).build());
        assertEquals(9, page.getSize());
        assertEquals(19, page.getTotal());
    }

    @TestConfiguration
    public static class Config {

        @Bean
        StorageAccessService storageAccessService() {
            return  new StorageAccessServiceMemory();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        StoragePathCoordinator storagePathCoordinator() {
            return new StoragePathCoordinator("/tt");
        }

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        AdminServerProperties adminServerProperties() {
            return new AdminServerProperties();
        }

        @Bean
        SecurityProperties securityProperties() {
            return new SecurityProperties();
        }

        @Bean
        HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
            return mock(HandlerMappingIntrospector.class);
        }

        @Bean
        SwTaskScheduler swTaskScheduler() {
            return mock(SwTaskScheduler.class);
        }

        @Bean
        ModelServingService modelServingService() {
            return mock(ModelServingService.class);
        }

        @Bean
        RunExecutor runExecutor() {
            return mock(RunExecutor.class);
        }
    }
}
