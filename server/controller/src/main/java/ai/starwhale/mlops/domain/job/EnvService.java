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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.runtime.BaseImageVO;
import ai.starwhale.mlops.api.protocol.runtime.DeviceVO;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.job.mapper.BaseImageMapper;
import ai.starwhale.mlops.domain.node.Device;
import cn.hutool.db.sql.Order;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnvService {

    @Resource
    private BaseImageMapper baseImageMapper;

    @Resource
    private BaseImageConvertor baseImageConvertor;


    public Long createImage(BaseImage baseImage) {
        BaseImageEntity entity = BaseImageEntity.builder()
            .id(baseImage.getId())
            .imageName(baseImage.getName())
            .build();
        baseImageMapper.createBaseImage(entity);
        return entity.getId();
    }

    public Boolean deleteImage(BaseImage baseImage) {
        int res;
        if(StringUtils.hasText(baseImage.getName())) {
            res = baseImageMapper.deleteBaseImageByName(baseImage.getName());
        } else {
            res = baseImageMapper.deleteBaseImage(baseImage.getId());
        }
        return res > 0;
    }

    public PageInfo<BaseImageVO> listImages(String namePrefix, PageParams pageParams, OrderParams orderParams) {

        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<BaseImageEntity> baseImageEntities = baseImageMapper.listBaseImages(namePrefix);

        return PageUtil.toPageInfo(baseImageEntities, baseImageConvertor::convert);
    }

    public List<DeviceVO> listDevices() {
        List<DeviceVO> list = new ArrayList<>();
        for(Device.Clazz cl : Device.Clazz.values()) {
            list.add(DeviceVO.builder()
                .id(String.valueOf(cl.getValue()))
                .name(cl.name())
                .build());
        }
        return list;
    }
}
