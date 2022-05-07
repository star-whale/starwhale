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

import ai.starwhale.mlops.common.IDConvertor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role implements GrantedAuthority {

    public static final String ADMIN = "USER_ADMIN";
    public static final String USER = "USER";

    private String id;
    private String authority;

    public Role fromEntity(RoleEntity entity) {
        return fromEntity(entity, null);
    }

    public Role fromEntity(RoleEntity entity, IDConvertor idConvertor) {
        if(entity == null) {
            return this;
        }
        if (idConvertor != null) {
            setId(idConvertor.convert(entity.getId()));
        }
        setAuthority(entity.getRoleName());
        return this;
    }
}
