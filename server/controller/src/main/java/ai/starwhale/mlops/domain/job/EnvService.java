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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.runtime.BaseImageVO;
import ai.starwhale.mlops.api.protocol.runtime.DeviceVO;
import ai.starwhale.mlops.domain.job.mapper.BaseImageMapper;
import ai.starwhale.mlops.domain.node.Device;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class EnvService {

    @Resource
    private BaseImageMapper baseImageMapper;

    @Resource
    private BaseImageConvertor baseImageConvertor;


    public List<BaseImageVO> listImages(String namePrefix) {
        List<BaseImageEntity> baseImageEntities = baseImageMapper.listBaseImages(namePrefix);

        return baseImageEntities.stream()
            .map(baseImageConvertor::convert)
            .collect(Collectors.toList());
    }

    public List<DeviceVO> listDevices() {
        List<DeviceVO> list = new ArrayList<>();
        for(Device.Clazz cl : Device.Clazz.values()) {
            list.add(DeviceVO.builder()
                .id(String.valueOf(cl.ordinal() + 1))
                .name(cl.name())
                .build());
        }
        return list;
    }
}
