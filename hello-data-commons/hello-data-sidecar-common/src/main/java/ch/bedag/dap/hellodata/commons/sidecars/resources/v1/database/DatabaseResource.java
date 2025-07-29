package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database;

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.Metadata;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database.response.superset.SupersetDatabase;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DatabaseResource implements HdResource {
    @EqualsAndHashCode.Include
    private final String apiVersion = "v1";
    @EqualsAndHashCode.Include
    private ModuleType moduleType;
    @EqualsAndHashCode.Include
    private final String kind = ModuleResourceKind.HELLO_DATA_DATABASES;
    @EqualsAndHashCode.Include
    private String instanceName;
    private Metadata metadata;
    private List<SupersetDatabase> data;

    /**
     * @param instanceName instance name
     * @param namespace    namespace
     * @param data         superset users information
     */
    public DatabaseResource(ModuleType  moduleType, String instanceName, String namespace, List<SupersetDatabase> data) {
        this.moduleType = moduleType;
        this.instanceName = instanceName;
        Map<String, Object> labels = new HashMap<>();
        labels.put(HD_MODULE_KEY, moduleType.getModuleName());
        this.metadata = new Metadata(instanceName, namespace, labels);

        this.data = data;
    }

    public DatabaseResource() {
        //NOOP default constructor for serialization
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
