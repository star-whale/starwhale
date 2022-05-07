/**
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

package ai.starwhale.mlops.agent.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.agent")
public class AgentProperties {

    private String version;

    /**
     * base dir path,default:/var/starwhale/
     */
    private String basePath;

    private String hostIP;

    private Task task;

    private Container container;

    @Data
    public static class Task {
        private int retryRunMaxNum = 10;
        private int retryRestartMaxNum = 10;
        private String pypiIndexUrl;
        private String pypiExtraIndexUrl;
        private String pypiTrustedHost;
    }

    @Data
    public static class Container {

        /**
         * docker host:(linux)unix:///var/run/docker.sock,(windows)npipe:////./pipe/docker_engine
         */
        private String host;
    }
}
