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

package ai.starwhale.mlops.api.protocol.event;


import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EventRequestTest {
    @Test
    public void testDeserialization() throws JsonProcessingException {
        String json = "{\"eventType\":\"INFO\",\"source\":\"CLIENT\",\"relatedResource\""
                + ":{\"eventResourceType\":\"JOB\",\"id\":1},\"message\":\"foo\",\"data\":\"bar\"}";
        var objMapper = new ObjectMapper();
        var event = objMapper.readValue(json, EventRequest.class);
        assertEquals(event.getEventType(), Event.EventType.INFO);
        assertEquals(event.getSource(), Event.EventSource.CLIENT);
        assertEquals(event.getRelatedResource().getEventResourceType(), Event.EventResourceType.JOB);
        assertEquals(event.getRelatedResource().getId(), 1L);
        assertEquals(event.getMessage(), "foo");
        assertEquals(event.getData(), "bar");
    }
}
