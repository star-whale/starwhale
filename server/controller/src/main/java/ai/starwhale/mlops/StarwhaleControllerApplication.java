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

package ai.starwhale.mlops;

import ai.starwhale.mlops.configuration.ControllerProperties;
import ai.starwhale.mlops.configuration.DataSourceProperties;
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAdminServer
@MapperScan({"ai.starwhale.mlops.domain.**.mapper"})
@EnableConfigurationProperties({ControllerProperties.class, RunTimeProperties.class, DockerSetting.class,
        DataSourceProperties.class, FeaturesProperties.class})
public class StarwhaleControllerApplication {

    public static void main(String[] args) {
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(
                StarwhaleControllerApplication.class);
        springApplicationBuilder.application().addListeners(new FailFast());
        springApplicationBuilder.run(args);
    }

    /**
     * sometimes JVM won't exit even the spring context is initialized with error
     */
    static class FailFast implements ApplicationListener<ApplicationFailedEvent> {

        @Override
        public void onApplicationEvent(ApplicationFailedEvent event) {
            System.exit(1);
        }
    }
}
