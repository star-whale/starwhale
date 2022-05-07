/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.runtime.BaseImageVO;
import ai.starwhale.mlops.api.protocol.runtime.DeviceVO;
import ai.starwhale.mlops.domain.job.EnvService;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class EnvController implements EnvApi{

    @Resource
    private EnvService envService;

    @Override
    public ResponseEntity<ResponseMessage<List<BaseImageVO>>> listBaseImage(String imageName) {
        List<BaseImageVO> baseImageVOS = envService.listImages(imageName);

        return ResponseEntity.ok(Code.success.asResponse(baseImageVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<DeviceVO>>> listDevice() {
        List<DeviceVO> deviceVOS = envService.listDevices();
        return ResponseEntity.ok(Code.success.asResponse(deviceVOS));
    }
}
