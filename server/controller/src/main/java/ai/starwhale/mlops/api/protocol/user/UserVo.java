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

package ai.starwhale.mlops.api.protocol.user;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "User object", title = "User")
@Validated
public class UserVo {

    @NotNull
    private String id;

    @NotNull
    private String name;

    @NotNull
    private Long createdTime;

    @NotNull
    private Boolean isEnabled;

    private String systemRole;

    private Map<String, String> projectRoles;

    public static UserVo empty() {
        return new UserVo("", "", -1L, false, "", Map.of());
    }

    public static UserVo fromEntity(UserEntity entity, IdConverter idConvertor) {
        if (entity == null) {
            return UserVo.empty();
        }
        return UserVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getUserName())
                .createdTime(entity.getCreatedTime().getTime())
                .isEnabled(entity.getUserEnabled() != null && entity.getUserEnabled() == 1)
                .build();
    }

    public static UserVo from(User user, IdConverter idConvertor) {
        if (user == null) {
            return UserVo.empty();
        }
        return UserVo.builder()
                .id(idConvertor.convert(user.getId()))
                .name(user.getName())
                .createdTime(user.getCreatedTime() == null ? null : user.getCreatedTime().getTime())
                .isEnabled(user.isEnabled())
                .build();
    }
}
