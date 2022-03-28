/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
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
