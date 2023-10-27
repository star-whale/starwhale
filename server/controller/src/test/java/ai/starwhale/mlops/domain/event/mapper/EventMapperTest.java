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

package ai.starwhale.mlops.domain.event.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.api.protocol.event.Event.EventResourceType;
import ai.starwhale.mlops.api.protocol.event.Event.EventSource;
import ai.starwhale.mlops.api.protocol.event.Event.EventType;
import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.event.po.EventEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class EventMapperTest extends MySqlContainerHolder {
    @Autowired
    private EventMapper eventMapper;

    @Test
    public void testEvents() {
        var events = eventMapper.listEventsOfResource(null, null);
        assertEquals(0, events.size());

        var entity = EventEntity.builder()
                .type(EventType.INFO)
                .source(EventSource.CLIENT)
                .resourceType(EventResourceType.JOB)
                .resourceId(1L)
                .message("foo")
                .data("{}")
                .createdTime(new Date(123L * 1000))
                .build();

        var entity2 = EventEntity.builder()
                .type(EventType.INFO)
                .source(EventSource.CLIENT)
                .resourceType(EventResourceType.TASK)
                .resourceId(2L)
                .message("bar")
                .data("{}")
                .createdTime(new Date(456L * 1000))
                .build();

        var entity3 = EventEntity.builder()
                .type(EventType.INFO)
                .source(EventSource.CLIENT)
                .resourceType(EventResourceType.TASK)
                .resourceId(3L)
                .message("baz")
                .data("{}")
                .createdTime(new Date(789L * 1000))
                .build();

        eventMapper.insert(entity);
        eventMapper.insert(entity2);
        eventMapper.insert(entity3);

        events = eventMapper.listEventsOfResource(EventResourceType.JOB, 1L);
        assertEquals(1, events.size());
        assertEquals(entity, events.get(0));

        events = eventMapper.listEventsOfResource(EventResourceType.TASK, 2L);
        assertEquals(1, events.size());
        assertEquals(entity2, events.get(0));

        events = eventMapper.listEventsOfResources(EventResourceType.TASK, List.of(2L, 3L));
        assertEquals(2, events.size());
        assertThat(events).containsExactly(entity2, entity3);
    }
}
