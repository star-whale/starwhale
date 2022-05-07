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

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserConvertor implements Convertor<UserEntity, UserVO> {

    @Resource private IDConvertor idConvertor;

    @Resource private RoleConvertor roleConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public UserVO convert(UserEntity entity) throws ConvertException {
      if(entity == null) {
          return UserVO.empty();
      }
      return UserVO.builder()
          .id(idConvertor.convert(entity.getId()))
          .name(entity.getUserName())
          .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
          .role(roleConvertor.convert(entity.getRole()))
          .isEnabled(entity.getUserEnabled() == 1)
          .build();
    }

    @Override
    public UserEntity revert(UserVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "UserVO");
        return UserEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .userName(vo.getName())
            .userEnabled(vo.getIsEnabled() ? 1 : 0)
            .role(roleConvertor.revert(vo.getRole()))
            .build();
    }
}
