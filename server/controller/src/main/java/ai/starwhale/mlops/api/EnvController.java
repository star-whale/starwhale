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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.runtime.BaseImageRequest;
import ai.starwhale.mlops.api.protocol.runtime.BaseImageVO;
import ai.starwhale.mlops.api.protocol.runtime.DeviceVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.bo.BaseImage;
import ai.starwhale.mlops.domain.job.EnvService;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class EnvController implements EnvApi{

    @Resource
    private EnvService envService;

    @Resource
    private IDConvertor idConvertor;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<BaseImageVO>>> listBaseImage(String imageName,
        Integer pageNum, Integer pageSize, String sort, Integer order) {
        PageInfo<BaseImageVO> baseImageVOS = envService.listImages(imageName,
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build(),
            OrderParams.builder()
                .sort(sort)
                .order(order)
                .build());

        return ResponseEntity.ok(Code.success.asResponse(baseImageVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<DeviceVO>>> listDevice() {
        List<DeviceVO> deviceVOS = envService.listDevices();
        return ResponseEntity.ok(Code.success.asResponse(deviceVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createImage(BaseImageRequest imageRequest) {
        Long id = envService.createImage(BaseImage.builder()
                .name(imageRequest.getImageName())
                .build());
        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(id)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteImage(String imageId) {
        Boolean res = envService.deleteImage(BaseImage.builder()
            .id(idConvertor.revert(imageId))
            .build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete baseImage failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }


}
