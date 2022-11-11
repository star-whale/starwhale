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

package ai.starwhale.mlops.domain.dataset.dataloader.dao;

import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.converter.SessionConverter;
import ai.starwhale.mlops.domain.dataset.dataloader.mapper.SessionMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SessionDao {
    private final SessionMapper mapper;
    private final SessionConverter converter;

    public SessionDao(SessionMapper mapper, SessionConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    public boolean insert(Session session) {
        return mapper.insert(converter.convert(session)) > 0;
    }

    public Session selectById(String sessionId) {
        var entity = mapper.selectById(sessionId);
        return entity != null ? converter.revert(entity) : null;
    }

    public List<Session> selectAll() {
        return mapper.select().stream().map(converter::revert).collect(Collectors.toList());
    }
}
