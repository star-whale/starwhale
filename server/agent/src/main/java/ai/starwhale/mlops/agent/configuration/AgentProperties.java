/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.agent")
public class AgentProperties {

    private Task task;
    private Container container;

    @Data
    public static class Task {

        /**
         * taskInfo dir path,default:/var/starwhale/task/
         */
        private String basePath;

    }

    @Data
    public static class Container {

        /**
         * docker host:(linux)unix:///var/run/docker.sock,(windows)npipe:////./pipe/docker_engine
         */
        private String host;
    }
}
