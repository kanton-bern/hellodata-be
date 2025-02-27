package ch.bedag.dap.hellodata.sidecars.sftpgo;

import ch.bedag.dap.hellodata.commons.nats.annotation.EnableJetStream;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.sidecars.sftpgo.config.S3ConnectionsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJetStream
@EnableScheduling
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
@ComponentScan("ch.bedag.dap.hellodata")
@EnableConfigurationProperties({HelloDataContextConfig.class, S3ConnectionsConfig.class})
public class HDSidecarSftpGo {

    public static void main(String[] args) {
        SpringApplication.run(HDSidecarSftpGo.class, args);
    }

    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder() {
            @Override
            public void configure(ObjectMapper objectMapper) {
                super.configure(objectMapper);
                objectMapper.registerModule(new JavaTimeModule());
            }
        };
    }


}
