package ai.starwhale.mlops.agent;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(AgentProperties.class)
public class StarWhaleAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarWhaleAgentApplication.class, args);
    }
}
