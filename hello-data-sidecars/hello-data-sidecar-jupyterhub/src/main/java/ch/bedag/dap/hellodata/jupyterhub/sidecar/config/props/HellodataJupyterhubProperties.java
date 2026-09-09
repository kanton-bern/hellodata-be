package ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "hello-data.jupyterhub")
public class HellodataJupyterhubProperties {
    private int tempUserPasswordValidInDays;
    private String dwhAdminUsername;
    private String dwhAdminPassword;
    private String dwhUrl;
    private String dwhDriverClassName;
    private List<String> dwhTempUserSchemas;
    /**
     * Shared group role that owns everything temporary users create and holds all their privileges.
     * Temporary users are only login credentials that are members of this role, so they can be dropped
     * with a plain DROP USER. PostgreSQL roles are cluster-global, so in a cluster shared by several data
     * domains this name must be unique per data domain (e.g. include the DWH database name).
     */
    private String dwhSharedRole;
    /**
     * Schema, owned via the shared role, where temporary users create shared read-write tables. Objects
     * there are owned by the shared role, so any temporary user (all of them admins) can manage them.
     */
    private String dwhSharedSchema;
}
