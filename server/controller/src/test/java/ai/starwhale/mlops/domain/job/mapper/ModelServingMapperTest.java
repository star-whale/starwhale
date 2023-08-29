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

package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;


@MybatisTest(properties = {"mybatis.configuration.map-underscore-to-camel-case=true"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ModelServingMapperTest extends MySqlContainerHolder {
    @Autowired
    private ModelServingMapper modelServingMapper;

    @Autowired
    private JobMapper jobMapper;

    private JobEntity jobRunning;
    private JobEntity jobCreated;
    private JobEntity jobReady;

    @BeforeEach
    public void setup() {
        jobRunning = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.RUNNING)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(1L)
                .resultOutputPath("").type(JobType.BUILT_IN)
                .stepSpec("stepSpec1")
                .projectId(1L).ownerId(1L).build();
        jobCreated = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.CREATED)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(1L)
                .resultOutputPath("").type(JobType.BUILT_IN)
                .stepSpec("stepSpec2")
                .devMode(true)
                .autoReleaseTime(new Date(123 * 1000L))
                .projectId(1L).ownerId(1L).build();
        jobReady = JobEntity.builder().jobUuid(UUID.randomUUID().toString()).jobStatus(JobStatus.READY)
                .resourcePool("rp").runtimeVersionId(1L).modelVersionId(1L)
                .resultOutputPath("").type(JobType.BUILT_IN)
                .stepSpec("stepSpec2")
                .devMode(true)
                .autoReleaseTime(new Date(123 * 1000L))
                .projectId(1L).ownerId(1L).build();
        jobMapper.addJob(jobRunning);
        jobMapper.addJob(jobCreated);
        jobMapper.addJob(jobReady);
    }

    @Test
    public void testFindByStatusIn() {

        var running = ModelServingEntity.builder()
                .modelVersionId(1L)
                .projectId(2L)
                .jobId(jobRunning.getId())
                .runtimeVersionId(3L)
                .ownerId(4L)
                .createUser("foo")
                .isDeleted(0)
                .resourcePool("bar")
                .lastVisitTime(new Date(System.currentTimeMillis() / 1000 * 1000))
                .build();
        var created = ModelServingEntity.builder()
                .modelVersionId(1L)
                .projectId(2L)
                .jobId(jobCreated.getId())
                .runtimeVersionId(3L)
                .ownerId(4L)
                .createUser("foo")
                .isDeleted(0)
                .resourcePool("bar")
                .lastVisitTime(new Date(System.currentTimeMillis() / 1000 * 1000))
                .build();
        var ready = ModelServingEntity.builder()
                .modelVersionId(1L)
                .projectId(2L)
                .jobId(jobReady.getId())
                .runtimeVersionId(3L)
                .ownerId(4L)
                .createUser("foo")
                .isDeleted(0)
                .resourcePool("bar")
                .lastVisitTime(new Date(System.currentTimeMillis() / 1000 * 1000))
                .build();
        modelServingMapper.add(running);
        modelServingMapper.add(created);
        modelServingMapper.add(ready);

        List<ModelServingEntity> runningServices = modelServingMapper.findByStatusIn(JobStatus.RUNNING);
        Assertions.assertEquals(1, runningServices.size());
        Assertions.assertEquals(JobStatus.RUNNING, runningServices.get(0).getJobStatus());
        Assertions.assertEquals(running.getId(), runningServices.get(0).getId());

        runningServices = modelServingMapper.findByStatusIn(JobStatus.CREATED, JobStatus.READY);
        Assertions.assertEquals(2, runningServices.size());
        List<ModelServingEntity> entities = runningServices.stream().sorted(
                Comparator.comparing(ModelServingEntity::getId)).collect(Collectors.toList());
        Assertions.assertIterableEquals(
                List.of(created.getId(), ready.getId()),
                entities.stream().map(ModelServingEntity::getId).collect(
                        Collectors.toList())
        );
        Assertions.assertEquals(JobStatus.CREATED, runningServices.get(0).getJobStatus());
        Assertions.assertEquals(JobStatus.READY, runningServices.get(1).getJobStatus());
    }

    @Test
    public void testGetAndSet() {
        var entity = ModelServingEntity.builder()
                .modelVersionId(1L)
                .jobId(jobRunning.getId())
                .jobStatus(JobStatus.RUNNING)
                .projectId(2L)
                .runtimeVersionId(3L)
                .ownerId(4L)
                .createUser("foo")
                .isDeleted(0)
                .resourcePool("bar")
                .lastVisitTime(new Date(System.currentTimeMillis() / 1000 * 1000))
                .build();
        modelServingMapper.add(entity);
        var id = entity.getId();
        var result = modelServingMapper.find(id);
        Assertions.assertEquals(entity, result);

        var another = ModelServingEntity.builder()
                .modelVersionId(2L)
                .projectId(entity.getProjectId())
                .jobId(jobCreated.getId())
                .jobStatus(JobStatus.CREATED)
                .runtimeVersionId(entity.getRuntimeVersionId())
                .ownerId(entity.getOwnerId())
                .createUser(entity.getCreateUser())
                .isDeleted(entity.getIsDeleted())
                .resourcePool(entity.getResourcePool())
                .lastVisitTime(entity.getLastVisitTime())
                .build();
        modelServingMapper.add(another);
        result = modelServingMapper.find(another.getId());
        Assertions.assertEquals(another, result);


        var list = modelServingMapper.list(null, null, null, null);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(id, list.get(0).getId());
        Assertions.assertEquals(JobStatus.RUNNING, list.get(0).getJobStatus());
        Assertions.assertEquals(JobStatus.CREATED, list.get(1).getJobStatus());

        list = modelServingMapper.list(null, 7L, null, null)
                .stream()
                .sorted(Comparator.comparing(ModelServingEntity::getId))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, list.size());
        list = modelServingMapper.list(2L, 1L, 3L, "bar");
        Assertions.assertEquals(1, list.size());

        // test updating last visit time
        var visit = new Date(1000);
        modelServingMapper.updateLastVisitTime(id, visit);
        result = modelServingMapper.find(id);
        Assertions.assertEquals(visit, result.getLastVisitTime());
        entity.setLastVisitTime(visit);
        Assertions.assertEquals(entity, result);
    }
}
