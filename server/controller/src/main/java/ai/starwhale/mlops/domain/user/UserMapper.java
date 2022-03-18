/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.user;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface UserMapper {

    Long createUser(@NotNull UserEntity user);

    UserEntity findUser(@NotNull Long id);

    List<UserEntity> listUsers(String userName);

    int changePassword(@NotNull Long id, @NotNull String password);

    int enableUser(@NotNull Long id, @NotNull Integer isEnabled);
}
