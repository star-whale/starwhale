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


import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ModelVersionMapperTest extends MySqlContainerHolder {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ModelVersionMapper modelVersionMapper;

    @Test
    public void testUnShareByProject() {
        var model = ModelEntity.builder()
                .projectId(1L)
                .ownerId(1L)
                .modelName("model1")
                .build();
        modelMapper.insert(model);
        var modelVersion = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("v1")
                .jobs("")
                .ownerId(1L)
                .shared(true)
                .build();
        modelVersionMapper.insert(modelVersion);

        var model2 = ModelEntity.builder()
                .projectId(2L)
                .ownerId(1L)
                .modelName("model2")
                .build();
        modelMapper.insert(model2);
        var modelVersion2 = ModelVersionEntity.builder()
                .modelId(model2.getId())
                .versionName("v2")
                .jobs("")
                .ownerId(1L)
                .shared(true)
                .build();
        modelVersionMapper.insert(modelVersion2);

        modelVersionMapper.unShareModelVersionWithinProject(model.getProjectId());
        var mv = modelVersionMapper.find(modelVersion.getId());
        assertEquals(false, mv.getShared());
        var mv2 = modelVersionMapper.find(modelVersion2.getId());
        assertEquals(true, mv2.getShared());
    }

    @Test
    public void testStorageSize() {
        var model = ModelEntity.builder()
                .projectId(1L)
                .ownerId(1L)
                .modelName("model1")
                .build();
        modelMapper.insert(model);
        var modelVersion = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("v1")
                .jobs("")
                .ownerId(1L)
                .storageSize(789L)
                .build();
        modelVersionMapper.insert(modelVersion);

        var mv = modelVersionMapper.find(modelVersion.getId());
        assertEquals(789L, mv.getStorageSize());

        // update
        modelVersion.setStorageSize(123L);
        modelVersionMapper.update(modelVersion);
        mv = modelVersionMapper.find(modelVersion.getId());
        assertEquals(123L, mv.getStorageSize());
    }

    @Test
    public void testUpdate() {
        var model = ModelEntity.builder()
                .projectId(1L)
                .ownerId(1L)
                .modelName("model1")
                .build();
        modelMapper.insert(model);
        var modelVersion = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("v1")
                .jobs("")
                .ownerId(1L)
                .build();
        modelVersionMapper.insert(modelVersion);

        var mv = modelVersionMapper.find(modelVersion.getId());
        assertEquals("v1", mv.getVersionName());

        // update
        modelVersion.setVersionTag("tag2");
        modelVersion.setBuiltInRuntime("runtime2");
        modelVersion.setJobs("job2");
        modelVersion.setMetaBlobId("blob2");
        modelVersion.setStorageSize(123L);
        modelVersionMapper.update(modelVersion);

        mv = modelVersionMapper.find(modelVersion.getId());
        assertEquals("tag2", mv.getVersionTag());
        assertEquals("runtime2", mv.getBuiltInRuntime());
        assertEquals("job2", mv.getJobs());
        assertEquals("blob2", mv.getMetaBlobId());
        assertEquals(123L, mv.getStorageSize());
    }

    @Test
    public void testList() {
        var model = ModelEntity.builder()
                .projectId(1L)
                .ownerId(1L)
                .modelName("model1")
                .build();
        modelMapper.insert(model);
        var modelVersion = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("v1")
                .jobs("")
                .ownerId(1L)
                .build();
        modelVersionMapper.insert(modelVersion);
        var modelVersionDraft = ModelVersionEntity.builder()
                .modelId(model.getId())
                .versionName("v2")
                .jobs("")
                .draft(true)
                .ownerId(1L)
                .build();
        modelVersionMapper.insert(modelVersionDraft);

        List<ModelVersionEntity> list = modelVersionMapper.list(model.getId(), null, false);
        Assertions.assertEquals(1, list.size());
        ModelVersionEntity mv = list.get(0);
        assertEquals("v1", mv.getVersionName());
        List<ModelVersionEntity> listDraft = modelVersionMapper.list(model.getId(), null, true);
        Assertions.assertEquals(1, listDraft.size());
        ModelVersionEntity mvD = listDraft.get(0);
        assertEquals("v2", mvD.getVersionName());
    }
}
