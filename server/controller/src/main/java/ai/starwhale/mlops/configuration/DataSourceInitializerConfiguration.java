package ai.starwhale.mlops.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class DataSourceInitializerConfiguration {

    @Value("classpath:sql/schema.sql")
    private Resource sqlScriptSchema;

//    @Value("classpath:sql/init-data.sql")
//    private Resource sqlScriptData;

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource){
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(databasePopulator());
        return dataSourceInitializer;
    }

    private DatabasePopulator databasePopulator(){
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(sqlScriptSchema);
//        populator.addScript(sqlScriptData);

        return populator;
    }
}
