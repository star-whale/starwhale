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

package ai.starwhale.mlops.domain.job.step.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class StepMapperTest extends MySqlContainerHolder {

    @Autowired
    private StepMapper stepMapper;

    @Test
    public void testSaveAndUpdateStatus() {
        StepEntity step1 = StepEntity.builder()
                .uuid("test_uuid")
                .name("test_step")
                .jobId(1L)
                .lastStepId(3L)
                .status(StepStatus.CREATED)
                .concurrency(3)
                .taskNum(10)
                .build();

        StepEntity step2 = StepEntity.builder()
                .uuid("test_uuid2")
                .name("test_step2")
                .jobId(1L)
                .lastStepId(2L)
                .status(StepStatus.CANCELED)
                .concurrency(4)
                .taskNum(20)
                .build();

        stepMapper.save(step1);
        stepMapper.save(step2);

        List<StepEntity> list = stepMapper.findByJobId(1L);
        Assertions.assertEquals(2, list.size());

        stepMapper.updateStatus(List.of(step1.getId(), step2.getId()), StepStatus.SUCCESS);
        list = stepMapper.findByJobId(1L);
        for (StepEntity stepEntity : list) {
            Assertions.assertEquals(StepStatus.SUCCESS, stepEntity.getStatus());
        }

    }

    @Test
    public void testUpdateFinishTime() {
        StepEntity step1 = StepEntity.builder()
                .uuid("test_uuid11")
                .name("test_step11")
                .jobId(2L)
                .lastStepId(5L)
                .status(StepStatus.CREATED)
                .concurrency(3)
                .taskNum(10)
                .build();

        stepMapper.save(step1);
        long now = System.currentTimeMillis() / 1000 * 1000;
        stepMapper.updateFinishedTime(step1.getId(), new Date(now));

        List<StepEntity> list = stepMapper.findByJobId(2L);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(now, list.get(0).getFinishedTime().getTime());
    }

    @Test
    public void testUpdateStartedTime() {
        StepEntity step1 = StepEntity.builder()
                .uuid("test_uuid22")
                .name("test_step22")
                .jobId(3L)
                .lastStepId(5L)
                .status(StepStatus.RUNNING)
                .concurrency(3)
                .taskNum(10)
                .build();

        stepMapper.save(step1);
        long now = System.currentTimeMillis() / 1000 * 1000;
        stepMapper.updateStartedTime(step1.getId(), new Date(now));

        List<StepEntity> list = stepMapper.findByJobId(3L);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(now, list.get(0).getStartedTime().getTime());
    }

    @Test
    public void testAddAndGetWithId() {
        var step = StepEntity.builder()
                .uuid("uuid")
                .name("name")
                .jobId(4L)
                .lastStepId(5L)
                .status(StepStatus.RUNNING)
                .concurrency(3)
                .taskNum(10)
                .originJson("{\"foo\": \"bar\"}")
                .build();

        stepMapper.save(step);
        var step2 = stepMapper.findById(step.getId());
        step.setCreatedTime(step2.getCreatedTime());
        step.setModifiedTime(step2.getModifiedTime());
        Assertions.assertEquals(step, step2);
    }
}
