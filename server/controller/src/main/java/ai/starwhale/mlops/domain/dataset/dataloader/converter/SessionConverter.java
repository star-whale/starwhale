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

package ai.starwhale.mlops.domain.dataset.dataloader.converter;

import ai.starwhale.mlops.common.Converter;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.po.SessionEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class SessionConverter implements Converter<Session, SessionEntity> {

    @Override
    public SessionEntity convert(Session session) throws ConvertException {
        return SessionEntity.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .batchSize(session.getBatchSize())
                .datasetName(session.getDatasetName())
                .datasetVersion(session.getDatasetVersion())
                .tableName(session.getTableName())
                .start(session.getStart())
                .startInclusive(session.isStartInclusive())
                .end(session.getEnd())
                .endInclusive(session.isEndInclusive())
                .status(session.getStatus())
                .createdTime(session.getCreatedTime())
                .build();
    }

    @Override
    public Session revert(SessionEntity session) throws ConvertException {
        return Session.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .batchSize(session.getBatchSize())
                .datasetName(session.getDatasetName())
                .datasetVersion(session.getDatasetVersion())
                .tableName(session.getTableName())
                .start(session.getStart())
                .startInclusive(session.isStartInclusive())
                .end(session.getEnd())
                .endInclusive(session.isEndInclusive())
                .status(session.getStatus())
                .createdTime(session.getCreatedTime())
                .build();
    }
}
