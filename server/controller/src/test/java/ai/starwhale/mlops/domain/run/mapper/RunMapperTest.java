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

package ai.starwhale.mlops.domain.run.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RunMapperTest extends MySqlContainerHolder {

    @Autowired
    private RunMapper runMapper;

    RunEntity runEntity;

    String runSpecStr;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        RunSpec runSpec = RunSpec.builder()
                .image("img")
                .command(new ContainerCommand(new String[]{"cmd"}, new String[]{"bash -c"}))
                .envs(Map.of("k", "v"))
                .requestedResources(List.of(RuntimeResource.builder().type("cpu").request(1.003f).limit(2.00f).build()))
                .resourcePool(ResourcePool.builder().name("xka").build())
                .build();
        runSpecStr = objectMapper.writeValueAsString(runSpec);
        runEntity = RunEntity.builder()
                .runSpec(runSpec
                )
                .logDir("logpath")
                .taskId(1L)
                .status(null)
                .build();
        runMapper.insert(runEntity);
        Assertions.assertNotNull(runEntity.getId());
    }

    @Test
    void list() throws JsonProcessingException {
        List<RunEntity> list = runMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(runSpecStr, objectMapper.writeValueAsString(list.get(0).getRunSpec()));
        Assertions.assertEquals("logpath", list.get(0).getLogDir());
        Assertions.assertEquals(1L, list.get(0).getTaskId());
        Assertions.assertNull(list.get(0).getStatus());
        runMapper.insert(RunEntity.builder()
                                 .runSpec(runEntity.getRunSpec())
                                 .logDir("logpath2")
                                 .taskId(1L)
                                 .status(null)
                                 .build());
        list = runMapper.list(1L);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(runSpecStr, objectMapper.writeValueAsString(list.get(1).getRunSpec()));
        Assertions.assertEquals("logpath2", list.get(1).getLogDir());
        Assertions.assertEquals(1L, list.get(1).getTaskId());
        Assertions.assertNull(list.get(1).getStatus());
    }

    @Test
    void get() throws JsonProcessingException {
        RunEntity runEntity = runMapper.get(this.runEntity.getId());
        Assertions.assertEquals(runSpecStr, objectMapper.writeValueAsString(runEntity.getRunSpec()));
        Assertions.assertEquals("logpath", runEntity.getLogDir());
        Assertions.assertEquals(1L, runEntity.getTaskId());
        Assertions.assertNull(runEntity.getStatus());
    }

    @Test
    void getForUpdate() throws JsonProcessingException {
        RunEntity runEntity = runMapper.getForUpdate(this.runEntity.getId());
        Assertions.assertEquals(runSpecStr, objectMapper.writeValueAsString(runEntity.getRunSpec()));
        Assertions.assertEquals("logpath", runEntity.getLogDir());
        Assertions.assertEquals(1L, runEntity.getTaskId());
        Assertions.assertNull(runEntity.getStatus());
    }


    @Test
    void update() {
        RunEntity runEntity = runMapper.getForUpdate(this.runEntity.getId());
        runEntity.setStatus(RunStatus.RUNNING);
        runEntity.setFailedReason("failedReason");
        long startTime = System.currentTimeMillis();
        runEntity.setStartTime(new Date(startTime));
        runEntity.setFinishTime(new Date(startTime + 3));
        runEntity.setIp("ip");
        runMapper.update(runEntity);
        runEntity = runMapper.get(this.runEntity.getId());
        Assertions.assertEquals(RunStatus.RUNNING, runEntity.getStatus());
        Assertions.assertEquals("failedReason", runEntity.getFailedReason());
        Assertions.assertEquals(startTime, runEntity.getStartTime().getTime());
        Assertions.assertEquals(startTime + 3, runEntity.getFinishTime().getTime());
        Assertions.assertEquals("ip", runEntity.getIp());
    }
}