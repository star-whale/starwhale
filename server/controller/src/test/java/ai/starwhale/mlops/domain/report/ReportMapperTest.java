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

package ai.starwhale.mlops.domain.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.report.mapper.ReportMapper;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;


@MybatisTest(properties = {
    "logging.level.root=DEBUG",
    "logging.level.ai.starwhale.mlops=DEBUG"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReportMapperTest extends MySqlContainerHolder {
    @Autowired
    private ReportMapper mapper;

    @Test
    public void test() {
        // insert
        var entity = ReportEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .title("title-1")
                .content("content")
                .projectId(1L)
                .ownerId(1L)
                .build();
        var res = mapper.insert(entity);
        assertTrue(res > 0);

        // select
        var selectRes = mapper.selectById(entity.getId());
        assertEntity(entity, selectRes);
        selectRes = mapper.selectByUuid(entity.getUuid());
        assertEntity(entity, selectRes);

        var list = mapper.selectByProject("", 1L);
        assertEquals(1, list.size());

        // update
        entity.setTitle("new title");
        entity.setDescription("new desc");
        res = mapper.update(entity);
        assertTrue(res > 0);
        selectRes = mapper.selectById(entity.getId());
        assertEntity(entity, selectRes);

        entity.setContent("new content");
        res = mapper.update(entity);
        assertTrue(res > 0);
        selectRes = mapper.selectById(entity.getId());
        assertEntity(entity, selectRes);

        // update shared
        mapper.updateShared(entity.getId(), true);
        selectRes = mapper.selectById(entity.getId());
        assertEquals(true, selectRes.getShared());

        // remove and recover
        mapper.remove(entity.getId());
        list = mapper.selectByProject("", 1L);
        assertEquals(0, list.size());

        mapper.recover(entity.getId());
        list = mapper.selectByProject("", 1L);
        assertEquals(1, list.size());

        // filter
        list = mapper.selectByProject("title", 1L);
        assertEquals(1, list.size());
        list = mapper.selectByProject("tle", 1L);
        assertEquals(1, list.size());
        list = mapper.selectByProject("title2", 1L);
        assertEquals(0, list.size());
    }

    private void assertEntity(ReportEntity source, ReportEntity target) {
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getContent(), target.getContent());
        assertEquals(source.getOwnerId(), target.getOwnerId());
        assertEquals(source.getProjectId(), target.getProjectId());
        assertEquals(source.getTitle(), target.getTitle());
        assertEquals(source.getDescription(), target.getDescription());
        assertEquals(source.getShared(), target.getShared());
    }
}
