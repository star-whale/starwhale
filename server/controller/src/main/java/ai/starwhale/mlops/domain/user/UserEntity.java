/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.common.BaseEntity;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity extends BaseEntity implements Serializable {

    private Long id;

    private String userName;

    private String userPwd;

    private String userPwdSalt;

    private Long roleId;

    private RoleEntity role;

    private Integer userEnabled;

}
