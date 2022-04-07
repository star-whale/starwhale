package ai.starwhale.mlops.configuration.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ObjectMapperConfig {

    @Bean
    ObjectMapper yamlMapper(){
        return new ObjectMapper(new YAMLFactory());
    }

    @Primary
    @Bean
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
