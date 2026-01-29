package ch.bedag.dap.hellodata.jupyterhub.gateway.entities;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Permissions implements Serializable {
    private List<String> portalPermissions;
}
