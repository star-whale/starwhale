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

package ai.starwhale.mlops.domain.runtime;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RuntimeManager {

    @Resource
    private RuntimeMapper runtimeMapper;
    @Resource
    private RuntimeVersionMapper runtimeVersionMapper;
    @Resource
    private IDConvertor idConvertor;

    @Resource
    private ProjectManager projectManager;

    public Long getRuntimeId(String runtimeUrl, String projectUrl) {
        if(idConvertor.isID(runtimeUrl)) {
            return idConvertor.revert(runtimeUrl);
        }
        Long projectId = projectManager.getProjectId(projectUrl);
        RuntimeEntity runtimeEntity = runtimeMapper.findByName(runtimeUrl, projectId);
        if(runtimeEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME)
                .tip(String.format("Unable to find Runtime %s", runtimeUrl)), HttpStatus.BAD_REQUEST);
        }
        return runtimeEntity.getId();
    }

    public Long getRuntimeVersionId(String versionUrl, Long runtimeId) {
        if(idConvertor.isID(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        RuntimeVersionEntity entity = runtimeVersionMapper.findByNameAndRuntimeId(versionUrl, runtimeId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME)
                .tip(String.format("Unable to find Runtime %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
