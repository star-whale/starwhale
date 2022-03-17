/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {

    private String modifyUser;
    private String createUser;

    private LocalDateTime createdTime;

    private LocalDateTime modifiedTime;
}
