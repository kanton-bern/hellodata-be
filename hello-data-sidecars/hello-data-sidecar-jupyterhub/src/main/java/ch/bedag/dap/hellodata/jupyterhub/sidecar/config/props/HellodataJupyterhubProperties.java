package ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hellodata.jupyterhub")
public class HellodataJupyterhubProperties {
    private int tempUserPasswordValidInDays;
    private String dwhAdminUsername;
    private String dwhAdminPassword;
    private String dwhUrl;
    private String dwhDriverClassName;
}
