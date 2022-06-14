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
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
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

    @Resource
    private ProjectManager projectManager;

    public Long getSWMPId(String swmpUrl, String projectUrl) {
        if(idConvertor.isID(swmpUrl)) {
            return idConvertor.revert(swmpUrl);
        }
        Long projectId = projectManager.getProjectId(projectUrl);
        SWModelPackageEntity entity = swmpMapper.findByName(swmpUrl, projectId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find swmp %s", swmpUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    public Long getSWMPVersionId(String versionUrl, Long swmpId) {
        if(idConvertor.isID(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        SWModelPackageVersionEntity entity = versionMapper.findByNameAndSwmpId(versionUrl, swmpId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find swmp %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
