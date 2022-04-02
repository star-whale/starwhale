/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "User object", title = "User")
@Validated
public class UserVO {

    private String id;

    private String name;

    private Long createdTime;

    private RoleVO role;

    private Boolean isEnabled;

    public static UserVO empty() {
        return new UserVO("", "", -1L, RoleVO.empty(), false);
    }
}
