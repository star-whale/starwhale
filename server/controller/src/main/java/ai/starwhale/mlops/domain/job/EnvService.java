/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
