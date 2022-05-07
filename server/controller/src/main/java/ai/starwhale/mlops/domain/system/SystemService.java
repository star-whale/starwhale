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
