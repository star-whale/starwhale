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

package ai.starwhale.mlops.domain.event;

import ai.starwhale.mlops.api.protocol.event.EventRequest;
import ai.starwhale.mlops.api.protocol.event.EventVo;
import ai.starwhale.mlops.domain.event.po.EventEntity;
import org.springframework.stereotype.Component;

@Component
public class EventConverter {
    public EventEntity toEntity(EventRequest request) {
        return EventEntity.builder()
                .type(request.getEventType())
                .source(request.getSource())
                .resourceType(request.getRelatedResource().getEventResourceType())
                .resourceId(request.getRelatedResource().getId())
                .message(request.getMessage())
                .data(request.getData())
                .build();
    }

    public EventVo toVo(EventEntity entity) {
        return EventVo.builder()
                .id(entity.getId())
                .eventType(entity.getType())
                .source(entity.getSource())
                .message(entity.getMessage())
                .data(entity.getData())
                .timestamp(entity.getCreatedTime().getTime())
                .build();
    }
}
