/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.system.mapper.AgentMapper;
import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    @Resource
    private AgentMapper agentMapper;

    @Resource
    private AgentConvertor agentConvertor;


    public List<AgentVO> listAgents(String ipPrefix, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<AgentEntity> agentEntities = agentMapper.listAgents();

        return agentEntities.stream()
            .map(agentConvertor::convert)
            .collect(Collectors.toList());
    }
}
