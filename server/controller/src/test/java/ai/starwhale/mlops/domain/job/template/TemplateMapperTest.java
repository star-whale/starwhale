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

package ai.starwhale.mlops.domain.job.template;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.template.mapper.TemplateMapper;
import ai.starwhale.mlops.domain.job.template.po.TemplateEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TemplateMapperTest extends MySqlContainerHolder {
    @Autowired
    private TemplateMapper mapper;

    @Test
    public void testInsertAndFind() {
        var entity = TemplateEntity.builder().name("name1").projectId(1L).jobId(2L).ownerId(1L).build();
        mapper.insert(entity);
        assertThrows(SwValidationException.class, () -> mapper.insert(entity));
        Assertions.assertEquals(entity, mapper.selectById(entity.getId()));
        Assertions.assertEquals(entity, mapper.selectByNameForUpdate(entity.getName(), entity.getProjectId()));
    }

    @Test
    public void testList() {
        var entity1 = TemplateEntity.builder().name("name1").projectId(1L).jobId(2L).ownerId(1L).build();
        var entity2 = TemplateEntity.builder().name("name2").projectId(1L).jobId(2L).ownerId(1L).build();
        mapper.insert(entity1);
        mapper.insert(entity2);
        Assertions.assertEquals(2, mapper.select(1L, 10).size());
        Assertions.assertEquals(2, mapper.selectAll(1L).size());
    }

    @Test
    public void testRemoveAndRecover() {
        var entity1 = TemplateEntity.builder().name("name1").projectId(1L).jobId(2L).ownerId(1L).build();
        mapper.insert(entity1);

        var duplicate = TemplateEntity.builder().name("name1").projectId(1L).jobId(2L).ownerId(1L).build();
        assertThrows(DuplicateKeyException.class, () -> mapper.insert(duplicate));

        mapper.remove(entity1.getId());
        Assertions.assertEquals(1, mapper.selectById(entity1.getId()).getIsDeleted());
        Assertions.assertNull(mapper.selectByNameForUpdate(entity1.getName(), entity1.getProjectId()));
        Assertions.assertEquals(entity1.getName(), mapper.selectDeletedById(entity1.getId()).getName());

        mapper.insert(duplicate);
        assertThrows(DuplicateKeyException.class, () -> mapper.recover(entity1.getId()));
        mapper.remove(duplicate.getId());

        mapper.recover(entity1.getId());
        Assertions.assertEquals(0, mapper.selectById(entity1.getId()).getIsDeleted());
        Assertions.assertEquals(entity1, mapper.selectByNameForUpdate(entity1.getName(), entity1.getProjectId()));
        Assertions.assertNull(mapper.selectDeletedById(entity1.getId()));

        assertThrows(DuplicateKeyException.class, () -> mapper.insert(duplicate));
    }
}
