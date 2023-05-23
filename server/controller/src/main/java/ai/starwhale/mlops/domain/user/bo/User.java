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

package ai.starwhale.mlops.domain.user.bo;

import ai.starwhale.mlops.configuration.security.JwtLoginToken;
import ai.starwhale.mlops.configuration.security.SwPasswordEncoder;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails, Serializable {

    private Long id;
    private String name;
    private String password;
    private String salt;
    private Date createdTime;
    private boolean active;
    private Set<? extends GrantedAuthority> roles;

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
        if (entity == null) {
            return this;
        }
        setId(entity.getId());
        setName(entity.getUserName());
        setPassword(entity.getUserPwd());
        setSalt(entity.getUserPwdSalt());
        setActive(entity.getUserEnabled() == 1);
        setCreatedTime(entity.getCreatedTime());
        return this;
    }

    /**
     * Check for some default information
     */
    public void defaultChecks() throws AccountStatusException {
        if (!isAccountNonLocked()) {
            throw new LockedException("User account is locked");
        }

        if (!isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        if (!isAccountNonExpired()) {
            throw new AccountExpiredException("User account has expired");
        }
    }

    /**
     * Check if the password is correct
     *
     */
    public void additionalAuthenticationChecks(JwtLoginToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        String presentedPassword = authentication.getCredentials().toString();
        PasswordEncoder passwordEncoder = SwPasswordEncoder.getEncoder(getSalt());
        if (!passwordEncoder.matches(presentedPassword, getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
    }
}
