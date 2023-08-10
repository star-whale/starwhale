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

package ai.starwhale.mlops.domain.user.converter;

import ai.starwhale.mlops.api.protobuf.User.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class UserVoConverter {

    private final IdConverter idConverter;

    public UserVoConverter(IdConverter idConverter) {
        this.idConverter = idConverter;
    }


    public UserVo convert(UserEntity entity) throws ConvertException {
        if (entity == null) {
            return UserVo.newBuilder().build();
        }
        return UserVo.newBuilder()
                .setId(idConverter.convert(entity.getId()))
                .setName(entity.getUserName())
                .setCreatedTime(entity.getCreatedTime().getTime())
                .setIsEnabled(entity.getUserEnabled() != null && entity.getUserEnabled() == 1)
                .build();
    }

    public UserVo fromBo(User user) {
        if (user == null) {
            return UserVo.newBuilder().build();
        }

        return UserVo.newBuilder()
                .setId(idConverter.convert(user.getId()))
                .setName(user.getName())
                .setCreatedTime(user.getCreatedTime().getTime())
                .setIsEnabled(user.isEnabled())
                .build();
    }
}
