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

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.api.protocol.agent.AgentVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AgentConvertor implements Convertor<AgentEntity, AgentVo> {

    @Resource
    private IdConvertor idConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public AgentVo convert(AgentEntity agentEntity) throws ConvertException {
        if (agentEntity == null) {
            return AgentVo.empty();
        }
        return AgentVo.builder()
                .id(idConvertor.convert(agentEntity.getId()))
                .ip(agentEntity.getAgentIp())
                .connectedTime(localDateTimeConvertor.convert(agentEntity.getConnectTime()))
                .status(agentEntity.getStatus())
                .version(agentEntity.getAgentVersion())
                .serialNumber(agentEntity.getSerialNumber())
                .build();
    }

    @Override
    @Deprecated
    public AgentEntity revert(AgentVo agentVo) throws ConvertException {
        Objects.requireNonNull(agentVo, "agentVo");
        return AgentEntity.builder()
                .id(idConvertor.revert(agentVo.getId()))
                .agentIp(agentVo.getIp())
                .serialNumber(agentVo.getSerialNumber())
                .build();
    }
}
