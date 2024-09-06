package ch.bedag.dap.hellodata.jupyterhub.gateway.entities;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class Permissions implements Serializable {
    private List<String> portalPermissions;
}
