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

    public ArbitralResource(ModuleType moduleType, String instanceName, Metadata metadata, Object data) {
        this.moduleType = moduleType;
        this.instanceName = instanceName;
        this.metadata = metadata;
        this.data = data;
    }

    public ArbitralResource(HdResource otherResource, boolean withData) {
        this.moduleType = otherResource.getModuleType();
        this.instanceName = otherResource.getInstanceName();
        this.metadata = otherResource.getMetadata();
        if (withData) {
            this.data = otherResource.getData();
        }
    }

    public ArbitralResource() {
        //NOOP default constructor for serialization
    }
}
