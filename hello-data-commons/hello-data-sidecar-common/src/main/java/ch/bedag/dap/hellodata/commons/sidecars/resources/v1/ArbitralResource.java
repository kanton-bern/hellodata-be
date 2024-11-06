package ch.bedag.dap.hellodata.commons.sidecars.resources.v1;

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ArbitralResource implements HdResource {
    @EqualsAndHashCode.Include
    private final String apiVersion = "v1";
    @EqualsAndHashCode.Include
    private final String kind = ModuleResourceKind.HELLO_DATA_ARBITRAL;
    @EqualsAndHashCode.Include
    private ModuleType moduleType;
    @EqualsAndHashCode.Include
    private String instanceName;
    private Metadata metadata;
    private Object data;


}
