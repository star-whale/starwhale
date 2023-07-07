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
}
