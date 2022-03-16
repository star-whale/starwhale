/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.jwt")
public class JwtProperties {
    /* jwt security */
    private String secret;

    /* jwt issuer */
    private String issuer = "starWhale";

    /* jwt expire time,by minutes */
    private Integer expireMinutes = 24 * 60;
}
