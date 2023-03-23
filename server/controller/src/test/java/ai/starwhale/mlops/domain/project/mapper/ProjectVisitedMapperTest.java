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

package ai.starwhale.mlops.domain.project.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ProjectVisitedMapperTest extends MySqlContainerHolder {

    @Autowired
    private ProjectVisitedMapper projectVisitedMapper;

    @BeforeEach
    public void setUp() {
        projectVisitedMapper.insert(1L, 1L);
        projectVisitedMapper.insert(1L, 2L);
        projectVisitedMapper.insert(1L, 3L);
        projectVisitedMapper.insert(2L, 3L);
        projectVisitedMapper.insert(2L, 4L);
        projectVisitedMapper.insert(1L, 1L);
    }

    @Test
    public void testList() {
        var res = projectVisitedMapper.listVisitedProjects(1L);
        Assertions.assertEquals(List.of(1L, 3L, 2L), res);

        res = projectVisitedMapper.listVisitedProjects(2L);
        Assertions.assertEquals(List.of(4L, 3L), res);
    }
}
