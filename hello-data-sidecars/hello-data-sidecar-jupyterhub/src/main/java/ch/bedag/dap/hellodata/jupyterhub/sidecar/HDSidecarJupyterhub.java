package ch.bedag.dap.hellodata.jupyterhub.sidecar;

import ch.bedag.dap.hellodata.commons.nats.annotation.EnableJetStream;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJetStream
@EnableScheduling
@ComponentScan("ch.bedag.dap.hellodata")
@SpringBootApplication
@EnableConfigurationProperties({HelloDataContextConfig.class})
public class HDSidecarJupyterhub {
    public static void main(String[] args) {
        SpringApplication.run(HDSidecarJupyterhub.class, args);
    }
}
