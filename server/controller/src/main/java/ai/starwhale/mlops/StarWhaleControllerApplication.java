/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops;

import ai.starwhale.mlops.configuration.ControllerProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("ai.starwhale.mlops.domain.*.mapper")
@EnableConfigurationProperties(ControllerProperties.class)
public class StarWhaleControllerApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(StarWhaleControllerApplication.class).run(args);
    }
}
