/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
