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

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.impl.DockerContainerClient;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Configuration
public class DockerConfiguration {

    @Bean
    public DockerClient dockerClient(AgentProperties agentProperties)
        throws URISyntaxException {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        URI dockerHost;
        if (agentProperties.getContainer() != null && StringUtils.hasText(agentProperties.getContainer().getHost())) {
            dockerHost = new URI(agentProperties.getContainer().getHost());
        } else {
            dockerHost = config.getDockerHost();
        }
        DockerHttpClient httpClient =  new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerHost)
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean
    public ContainerClient containerClient(DockerClient dockerClient) {
        return new DockerContainerClient(dockerClient);
    }
}
