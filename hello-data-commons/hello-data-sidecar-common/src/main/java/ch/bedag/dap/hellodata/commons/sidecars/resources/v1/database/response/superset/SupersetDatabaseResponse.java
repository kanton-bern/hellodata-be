package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.base.BaseSupersetResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupersetDatabaseResponse extends BaseSupersetResponse {
    private List<SupersetDatabase> result;//NOSONAR
}
