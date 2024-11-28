package ch.bedag.dap.hellodata.jupyterhub.sidecar.config.datasource;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static java.util.concurrent.TimeUnit.MINUTES;

@Configuration
@RequiredArgsConstructor
public class DwhJdbcTemplateConfig {
    private final HellodataJupyterhubProperties hellodataJupyterhubProperties;

    @Bean
    public DataSource customDwhDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(hellodataJupyterhubProperties.getDwhUrl());
        hikariConfig.setUsername(hellodataJupyterhubProperties.getDwhAdminUsername());
        hikariConfig.setPassword(hellodataJupyterhubProperties.getDwhAdminPassword());
        hikariConfig.setDriverClassName(hellodataJupyterhubProperties.getDwhDriverClassName());
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setMaxLifetime(MINUTES.toMillis(15));
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        return hikariDataSource;

    }

    @Bean
    public JdbcTemplate dwhJdbcTemplate() {
        return new JdbcTemplate(customDwhDataSource());
    }
}
