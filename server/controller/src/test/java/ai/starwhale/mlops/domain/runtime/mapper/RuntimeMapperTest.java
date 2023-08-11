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

package ai.starwhale.mlops.domain.runtime.mapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DuplicateKeyException;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class RuntimeMapperTest extends MySqlContainerHolder {

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Test
    public void testInsertAndFind() {
        RuntimeEntity entity = RuntimeEntity.builder()
                .runtimeName("runtime1")
                .ownerId(1L)
                .projectId(2L)
                .build();
        runtimeMapper.insert(entity);
        entity.setIsDeleted(0);

        Assertions.assertEquals(entity, runtimeMapper.find(entity.getId()));
        Assertions.assertEquals(entity, runtimeMapper.findByName(entity.getRuntimeName(),
                entity.getProjectId(), false));
        Assertions.assertEquals(entity, runtimeMapper.findByName(entity.getRuntimeName(),
                entity.getProjectId(), true));
    }

    @Test
    public void testList() {
        RuntimeEntity runtime1 = RuntimeEntity.builder()
                .runtimeName("runtime1")
                .ownerId(1L)
                .projectId(1L)
                .isDeleted(0)
                .build();
        RuntimeEntity runtime2 = RuntimeEntity.builder()
                .runtimeName("runtime2")
                .ownerId(2L)
                .projectId(2L)
                .isDeleted(0)
                .build();
        RuntimeEntity runtime3 = RuntimeEntity.builder()
                .runtimeName("runtime3")
                .ownerId(2L)
                .projectId(1L)
                .isDeleted(0)
                .build();

        runtimeMapper.insert(runtime1);
        runtimeMapper.insert(runtime2);
        runtimeMapper.insert(runtime3);

        var list = runtimeMapper.list(1L, null, null, null);
        Assertions.assertIterableEquals(List.of(runtime3, runtime1), list);

        list = runtimeMapper.list(2L, null, null, null);
        Assertions.assertIterableEquals(List.of(runtime2), list);

        list = runtimeMapper.list(null, "runtime", null, null);
        Assertions.assertIterableEquals(List.of(runtime3, runtime2, runtime1), list);

        list = runtimeMapper.list(null, "runtime1", null, null);
        Assertions.assertIterableEquals(List.of(runtime1), list);

        list = runtimeMapper.list(null, null, 2L, null);
        Assertions.assertIterableEquals(List.of(runtime3, runtime2), list);
    }

    @Test
    public void testRemoveAndRecover() {
        RuntimeEntity runtime1 = RuntimeEntity.builder()
                .runtimeName("runtime1")
                .ownerId(1L)
                .projectId(1L)
                .isDeleted(0)
                .build();
        runtimeMapper.insert(runtime1);
        RuntimeEntity duplicate = RuntimeEntity.builder()
                .runtimeName("runtime1")
                .ownerId(1L)
                .projectId(1L)
                .build();
        assertThrows(DuplicateKeyException.class, () -> runtimeMapper.insert(duplicate));

        runtimeMapper.remove(runtime1.getId());
        Assertions.assertEquals(1, runtimeMapper.find(runtime1.getId()).getIsDeleted());
        Assertions.assertNull(runtimeMapper.findByName(runtime1.getRuntimeName(), runtime1.getProjectId(), false));
        Assertions.assertEquals(runtime1.getRuntimeName(),
                runtimeMapper.findDeleted(runtime1.getId()).getRuntimeName());

        runtimeMapper.insert(duplicate);
        assertThrows(DuplicateKeyException.class, () -> runtimeMapper.recover(runtime1.getId()));
        runtimeMapper.remove(duplicate.getId());

        runtimeMapper.recover(runtime1.getId());
        Assertions.assertEquals(0, runtimeMapper.find(runtime1.getId()).getIsDeleted());
        Assertions.assertEquals(runtime1,
                runtimeMapper.findByName(runtime1.getRuntimeName(), runtime1.getProjectId(), false));
        Assertions.assertNull(runtimeMapper.findDeleted(runtime1.getId()));

        assertThrows(DuplicateKeyException.class, () -> runtimeMapper.insert(duplicate));
    }
}
