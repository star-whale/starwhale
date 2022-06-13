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

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.swmp.bo.SWMPObject;
import ai.starwhale.mlops.domain.swmp.bo.SWMPVersion;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SwmpManager {

    @Resource
    private SWModelPackageMapper swmpMapper;
    @Resource
    private SWModelPackageVersionMapper versionMapper;
    @Resource
    private IDConvertor idConvertor;

    public SWMPObject fromUrl(String runtimeUrl) {
        if(idConvertor.isID(runtimeUrl)) {
            return SWMPObject.builder().id(idConvertor.revert(runtimeUrl)).build();
        } else {
            return SWMPObject.builder().name(runtimeUrl).build();
        }
    }

    public SWMPVersion fromVersionUrl(String versionUrl) {
        if(idConvertor.isID(versionUrl)) {
            return SWMPVersion.builder().id(idConvertor.revert(versionUrl)).build();
        } else {
            return SWMPVersion.builder().name(versionUrl).build();
        }
    }

    public SWModelPackageEntity findSWMP(String swmpUrl) {
        return findSWMP(fromUrl(swmpUrl));
    }

    public SWModelPackageEntity findSWMP(SWMPObject swmpObject) {
        if(swmpObject.getId() != null) {
            return swmpMapper.findSWModelPackageById(swmpObject.getId());
        } else if (!StrUtil.isEmpty(swmpObject.getName())) {
            return swmpMapper.findByName(swmpObject.getName());
        }
        return null;
    }

    public Long getSWMPId(String swmpUrl) {
        SWMPObject swmpObject = fromUrl(swmpUrl);
        if(swmpObject.getId() != null) {
            return swmpObject.getId();
        }
        SWModelPackageEntity entity = swmpMapper.findByName(swmpObject.getName());
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find swmp %s", swmpUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    public Long getSWMPVersionId(String versionUrl, Long swmpId) {
        SWMPVersion version = fromVersionUrl(versionUrl);
        if(version.getId() != null) {
            return version.getId();
        }
        SWModelPackageVersionEntity entity = versionMapper.findByNameAndSwmpId(version.getName(), swmpId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find Runtime %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
