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

import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import java.util.Date;
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
        var entity = ModelServingEntity.builder()
                .modelVersionId(1L)
                .projectId(2L)
                .runtimeVersionId(3L)
                .ownerId(4L)
                .createUser("foo")
                .isDeleted(0)
                .resourcePool("bar")
                .jobStatus(JobStatus.RUNNING)
                .lastVisitTime(new Date(System.currentTimeMillis() / 1000 * 1000))
                .build();
        modelServingMapper.add(entity);
        var id = entity.getId();
        var result = modelServingMapper.find(id);
        Assertions.assertEquals(entity, result);

        var another = ModelServingEntity.builder()
                .modelVersionId(2L)
                .projectId(entity.getProjectId())
                .runtimeVersionId(entity.getRuntimeVersionId())
                .ownerId(entity.getOwnerId())
                .createUser(entity.getCreateUser())
                .isDeleted(entity.getIsDeleted())
                .resourcePool(entity.getResourcePool())
                .jobStatus(entity.getJobStatus())
                .lastVisitTime(entity.getLastVisitTime())
                .build();
        modelServingMapper.add(another);
        result = modelServingMapper.find(another.getId());
        Assertions.assertEquals(another, result);


        var list = modelServingMapper.list(null, null, null, null);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(id, list.get(0).getId());

        list = modelServingMapper.list(null, 7L, null, null);
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
