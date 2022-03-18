/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVO;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVO;
import com.github.pagehelper.PageInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController implements SystemApi{

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<AgentVO>>> listAgent(String ip, Integer pageNum,
        Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> systemVersionAction(String action) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<SystemVersionVO>> getCurrentVersion() {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<SystemVersionVO>> getLatestVersion() {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<UpgradeProgressVO>> getUpgradeProgress() {
        return null;
    }
}
