/**
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

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AgentConvertor implements Convertor<AgentEntity, AgentVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public AgentVO convert(AgentEntity agentEntity) throws ConvertException {
        if(agentEntity == null) {
            return AgentVO.empty();
        }
        return AgentVO.builder()
            .id(idConvertor.convert(agentEntity.getId()))
            .ip(agentEntity.getAgentIp())
            .connectedTime(localDateTimeConvertor.convert(agentEntity.getCreatedTime()))
            .version(agentEntity.getAgentVersion())
            .build();
    }

    @Override
    @Deprecated
    public AgentEntity revert(AgentVO agentVO) throws ConvertException {
        Objects.requireNonNull(agentVO, "agentVO");
        return AgentEntity.builder()
            .id(idConvertor.revert(agentVO.getId()))
            .agentIp(agentVO.getIp())
            .build();
    }
}
