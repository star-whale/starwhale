/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
