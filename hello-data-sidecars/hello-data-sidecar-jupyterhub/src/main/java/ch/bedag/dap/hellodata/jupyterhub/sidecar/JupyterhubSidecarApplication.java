package ch.bedag.dap.hellodata.jupyterhub.sidecar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@ComponentScan("ch.bedag.dap.hellodata")
@SpringBootApplication
public class JupyterhubSidecarApplication {
    public static void main(String[] args) {
        SpringApplication.run(JupyterhubSidecarApplication.class, args);
    }
}
