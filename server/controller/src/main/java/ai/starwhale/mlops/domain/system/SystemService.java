/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.system.agent.AgentCache;
import ai.starwhale.mlops.domain.system.agent.AgentConverter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    @Resource
    private AgentCache agentCache;

    @Resource
    private AgentConvertor agentConvertor;

    @Resource
    private AgentConverter agentConverter;

    @Value("${sw.version}")
    private String controllerVersion;

    public PageInfo<AgentVO> listAgents(String ipPrefix, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<AgentEntity> agents = agentCache.agents().stream().map(agentConverter::toEntity).collect(
            Collectors.toList());
        return PageUtil.toPageInfo(agents, agentConvertor::convert);
    }

    public String controllerVersion(){
        return controllerVersion;
    }
}
