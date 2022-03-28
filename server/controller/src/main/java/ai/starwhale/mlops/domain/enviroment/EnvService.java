/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.enviroment;

import ai.starwhale.mlops.api.protocol.job.BaseImageVO;
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
}
