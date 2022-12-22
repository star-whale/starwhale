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
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import org.junit.jupiter.api.Assertions;
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

    @Test
    public void testGetAndSet() {
        var project = 2L;
        var entity = ModelServingEntity.builder()
                .modelVersionId(1L)
                .projectId(project)
                .runtimeVersionId(3L)
                .ownerId(4L)
                .createUser("foo")
                .isDeleted(0)
                .resourcePool("bar")
                .jobStatus(JobStatus.RUNNING)
                .build();
        modelServingMapper.add(entity);
        var result = modelServingMapper.find(entity.getId());
        Assertions.assertEquals(entity, result);

        var entities = modelServingMapper.list(project);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals(entity, entities.get(0));

        Assertions.assertEquals(0, modelServingMapper.list(project + 1).size());

        modelServingMapper.delete(entity.getId());
        Assertions.assertEquals(0, modelServingMapper.list(project).size());
    }
}
