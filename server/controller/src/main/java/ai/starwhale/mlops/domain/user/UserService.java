/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.user;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    public Optional<User> findByUsername(String username) {
        // todo example, but you need confirm set value for the property:active
        return Optional.of(User.builder().name(username).password("$2a$10$USw2j1KtbjM2tQZtZY44tuOOrhE56IhhP/.1wVQzskJG6t0aRlBLe").roles(Set.of(new Role(Role.USER))).active(true).build());
    }
}
