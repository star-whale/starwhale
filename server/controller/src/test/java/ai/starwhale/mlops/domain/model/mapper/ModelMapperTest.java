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

package ai.starwhale.mlops.domain.model.mapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
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
public class ModelMapperTest extends MySqlContainerHolder {

    @Autowired
    private ModelMapper modelMapper;

    @Test
    public void testInsertAndFind() {
        ModelEntity entity = ModelEntity.builder()
                .modelName("model1")
                .ownerId(1L)
                .projectId(2L)
                .build();
        modelMapper.insert(entity);
        entity.setIsDeleted(0);

        Assertions.assertEquals(entity, modelMapper.find(entity.getId()));
        Assertions.assertEquals(entity, modelMapper.findByName(entity.getModelName(),
                entity.getProjectId(), false));
        Assertions.assertEquals(entity, modelMapper.findByName(entity.getModelName(),
                entity.getProjectId(), true));
    }

    @Test
    public void testList() {
        ModelEntity model1 = ModelEntity.builder()
                .modelName("model1")
                .ownerId(1L)
                .projectId(1L)
                .isDeleted(0)
                .build();
        ModelEntity model2 = ModelEntity.builder()
                .modelName("model2")
                .ownerId(2L)
                .projectId(2L)
                .isDeleted(0)
                .build();
        ModelEntity model3 = ModelEntity.builder()
                .modelName("model3")
                .ownerId(2L)
                .projectId(1L)
                .isDeleted(0)
                .build();

        modelMapper.insert(model1);
        modelMapper.insert(model2);
        modelMapper.insert(model3);

        var list = modelMapper.list(1L, null, null, null);
        Assertions.assertIterableEquals(List.of(model3, model1), list);

        list = modelMapper.list(2L, null, null, null);
        Assertions.assertIterableEquals(List.of(model2), list);

        list = modelMapper.list(null, "model", null, null);
        Assertions.assertIterableEquals(List.of(model3, model2, model1), list);

        list = modelMapper.list(null, "model1", null, null);
        Assertions.assertIterableEquals(List.of(model1), list);

        list = modelMapper.list(null, null, 2L, null);
        Assertions.assertIterableEquals(List.of(model3, model2), list);
    }

    @Test
    public void testRemoveAndRecover() {
        ModelEntity model1 = ModelEntity.builder()
                .modelName("model1")
                .ownerId(1L)
                .projectId(1L)
                .isDeleted(0)
                .build();
        modelMapper.insert(model1);

        ModelEntity duplicate = ModelEntity.builder()
                .modelName("model1")
                .ownerId(1L)
                .projectId(1L)
                .build();
        assertThrows(DuplicateKeyException.class, () -> modelMapper.insert(duplicate));

        modelMapper.remove(model1.getId());
        Assertions.assertEquals(1, modelMapper.find(model1.getId()).getIsDeleted());
        Assertions.assertNull(modelMapper.findByName(model1.getModelName(), model1.getProjectId(), false));
        Assertions.assertEquals(model1.getModelName(), modelMapper.findDeleted(model1.getId()).getModelName());

        modelMapper.insert(duplicate);
        assertThrows(DuplicateKeyException.class, () -> modelMapper.recover(model1.getId()));
        modelMapper.remove(duplicate.getId());

        modelMapper.recover(model1.getId());
        Assertions.assertEquals(0, modelMapper.find(model1.getId()).getIsDeleted());
        Assertions.assertEquals(model1, modelMapper.findByName(model1.getModelName(), model1.getProjectId(), false));
        Assertions.assertNull(modelMapper.findDeleted(model1.getId()));

        assertThrows(DuplicateKeyException.class, () -> modelMapper.insert(duplicate));
    }
}
