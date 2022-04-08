/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.common.IDConvertor;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails, Serializable {

    private String id;
    private Long idTableKey;
    private String name;
    private String password;
    private String salt;
    private boolean active;
    private Set<Role> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public User fromEntity(UserEntity entity) {
        return fromEntity(entity, null);
    }
    public User fromEntity(UserEntity entity, IDConvertor idConvertor) {
        if(entity == null) {
            return this;
        }
        if (idConvertor != null) {
          setId(idConvertor.convert(entity.getId()));
        }
        setName(entity.getUserName());
        setPassword(entity.getUserPwd());
        setSalt(entity.getUserPwdSalt());
        setActive(entity.getUserEnabled() == 1);
        setRoles(Set.of(new Role().fromEntity(entity.getRole(), idConvertor)));
        setIdTableKey(entity.getId());
        return this;
    }

}
