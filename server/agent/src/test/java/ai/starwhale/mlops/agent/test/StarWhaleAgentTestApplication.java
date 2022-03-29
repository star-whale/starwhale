package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "ai.starwhale.mlops.agent.**")
@EnableFeignClients
@EnableConfigurationProperties(AgentProperties.class)
public class StarWhaleAgentTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarWhaleAgentTestApplication.class, args);
    }
}
