package ch.bedag.dap.hellodata.jupyterhub.sidecar.config.datasource;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DwhJdbcTemplateConfig {
    private final HellodataJupyterhubProperties hellodataJupyterhubProperties;

    @Bean
    public DataSource customDwhDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(hellodataJupyterhubProperties.getDwhUrl());
        dataSource.setUsername(hellodataJupyterhubProperties.getDwhAdminUsername());
        dataSource.setPassword(hellodataJupyterhubProperties.getDwhAdminPassword());
        dataSource.setDriverClassName(hellodataJupyterhubProperties.getDwhDriverClassName());
        return dataSource;
    }

    @Bean
    public JdbcTemplate dwhJdbcTemplate() {
        return new JdbcTemplate(customDwhDataSource());
    }
}
