/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.job.BaseImageVO;
import ai.starwhale.mlops.api.protocol.agent.DeviceVO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvnController implements EnvApi{

    @Override
    public ResponseEntity<ResponseMessage<List<BaseImageVO>>> listBaseImage(String imageName) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<List<DeviceVO>>> listDevice() {
        return null;
    }
}
