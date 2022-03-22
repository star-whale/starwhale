/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtLoginToken extends AbstractAuthenticationToken {

    /** Login user info */
    private final Object principal;
    /** Password */
    private final Object credentials;

    /**
     * Create an unauthenticated authorization token, where the incoming principal is the username
     */
    public JwtLoginToken(Object principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    /**
     * Create an authenticated authorization token. This method should be called by AuthenticationProvider.
     * The parameter principal should be a UserDetails object returned from userService
     * @param principal the UserDetails found from userService
     * @param credentials
     * @param authorities
     */
    public JwtLoginToken(Object principal, Object credentials,
        Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

}
