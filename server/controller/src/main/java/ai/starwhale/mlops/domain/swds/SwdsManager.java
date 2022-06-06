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

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SwdsManager {

    @Resource
    private SWDatasetMapper datasetMapper;

    @Resource
    private SWDatasetVersionMapper datasetVersionMapper;

    @Resource
    private IDConvertor idConvertor;

    public SWDSObject fromUrl(String runtimeUrl) {
        if(idConvertor.isID(runtimeUrl)) {
            return SWDSObject.builder().id(idConvertor.revert(runtimeUrl)).build();
        } else {
            return SWDSObject.builder().name(runtimeUrl).build();
        }
    }

    public SWDSVersion fromVersionUrl(String versionUrl) {
        if(idConvertor.isID(versionUrl)) {
            return SWDSVersion.builder().id(idConvertor.revert(versionUrl)).build();
        } else {
            return SWDSVersion.builder().name(versionUrl).build();
        }
    }

    public SWDatasetEntity findSWDS(String swdsUrl) {
        return findSWDS(fromUrl(swdsUrl));
    }

    public SWDatasetEntity findSWDS(SWDSObject swdsObject) {
        if(swdsObject.getId() != null) {
            return datasetMapper.findDatasetById(swdsObject.getId());
        } else if (!StrUtil.isEmpty(swdsObject.getName())) {
            return datasetMapper.findByName(swdsObject.getName());
        }
        return null;
    }

    public Long getSWDSId(String swdsUrl) {
        SWDSObject obj = fromUrl(swdsUrl);
        if(obj.getId() != null) {
            return obj.getId();
        }
        SWDatasetEntity entity = datasetMapper.findByName(obj.getName());
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip(String.format("Unable to find swds %s", swdsUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    public Long getSWDSVersionId(String versionUrl, Long swdsId) {
        SWDSVersion version = fromVersionUrl(versionUrl);
        if(version.getId() != null) {
            return version.getId();
        }
        SWDatasetVersionEntity entity = datasetVersionMapper.findByDSIdAndVersionName(swdsId, version.getName());
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find Runtime %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

}
