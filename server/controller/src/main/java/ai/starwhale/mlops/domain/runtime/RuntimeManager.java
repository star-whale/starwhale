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
import ai.starwhale.mlops.domain.runtime.bo.Runtime;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
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

    public Runtime fromUrl(String runtimeUrl) {
        if(idConvertor.isID(runtimeUrl)) {
            return Runtime.builder().id(idConvertor.revert(runtimeUrl)).build();
        } else {
            return Runtime.builder().name(runtimeUrl).build();
        }
    }

    public RuntimeVersion fromVersionUrl(String runtimeVersionUrl) {
        if(idConvertor.isID(runtimeVersionUrl)) {
            return RuntimeVersion.builder().id(idConvertor.revert(runtimeVersionUrl)).build();
        } else {
            return RuntimeVersion.builder().versionName(runtimeVersionUrl).build();
        }
    }

    public RuntimeEntity findRuntime(String runtimeUrl) {
        return findRuntime(fromUrl(runtimeUrl));
    }

    public RuntimeEntity findRuntime(Runtime runtime) {
        if(runtime.getId() != null) {
            return runtimeMapper.findRuntimeById(runtime.getId());
        } else if (!StrUtil.isEmpty(runtime.getName())) {
            return runtimeMapper.findByName(runtime.getName());
        }
        return null;
    }

    public Long getRuntimeId(String runtimeUrl) {
        Runtime runtime = fromUrl(runtimeUrl);
        if(runtime.getId() != null) {
            return runtime.getId();
        }
        RuntimeEntity runtimeEntity = runtimeMapper.findByName(runtime.getName());
        if(runtimeEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME)
                .tip(String.format("Unable to find Runtime %s", runtimeUrl)), HttpStatus.BAD_REQUEST);
        }
        return runtimeEntity.getId();
    }

    public Long getRuntimeVersionId(String runtimeVersionUrl, Long runtimeId) {
        RuntimeVersion version = fromVersionUrl(runtimeVersionUrl);
        if(version.getId() != null) {
            return version.getId();
        }
        RuntimeVersionEntity entity = runtimeVersionMapper.findByNameAndRuntimeId(version.getVersionName(), runtimeId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME)
                .tip(String.format("Unable to find Runtime %s", runtimeVersionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
