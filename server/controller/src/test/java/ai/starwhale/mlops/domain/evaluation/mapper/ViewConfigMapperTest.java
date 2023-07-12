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

package ai.starwhale.mlops.domain.evaluation.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ViewConfigMapperTest extends MySqlContainerHolder {

    @Autowired
    private ViewConfigMapper viewConfigMapper;

    @Test
    public void testInsertAndFind() {
        ViewConfigEntity viewConfigEntity = ViewConfigEntity.builder()
                .configName("test_name")
                .content("content1")
                .ownerId(2L)
                .projectId(3L)
                .build();

        viewConfigMapper.createViewConfig(viewConfigEntity);

        ViewConfigEntity find = viewConfigMapper.findViewConfig(3L, "test_name");
        Assertions.assertEquals("content1", find.getContent());

        // insert with the same project id and name, will replace the old one
        var newViewConfigEntity = ViewConfigEntity.builder()
                .configName("test_name")
                .content("content2")
                .ownerId(2L)
                .projectId(3L)
                .build();
        viewConfigMapper.createViewConfig(newViewConfigEntity);
        find = viewConfigMapper.findViewConfig(3L, "test_name");
        Assertions.assertEquals("content2", find.getContent());
    }
}
