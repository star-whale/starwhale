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
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Role object", title = "Role")
@Validated
public class RoleVo {

    @NotNull
    private String id;

    @NotNull
    private String name;

    @NotNull
    private String code;

    private String description;

    public static RoleVo empty() {
        return new RoleVo("", "", "", "");
    }

    public static RoleVo fromEntity(RoleEntity roleEntity, IdConverter idConvertor) {
        if (roleEntity == null) {
            return RoleVo.empty();
        }
        return RoleVo.builder()
                .id(idConvertor.convert(roleEntity.getId()))
                .name(roleEntity.getRoleName())
                .code(roleEntity.getRoleCode())
                .description(roleEntity.getRoleDescription())
                .build();
    }

    public static RoleVo fromBo(Role role, IdConverter idConvertor) {
        if (role == null) {
            return RoleVo.empty();
        }
        return RoleVo.builder()
                .id(idConvertor.convert(role.getId()))
                .name(role.getRoleName())
                .code(role.getRoleCode())
                .build();
    }
}
