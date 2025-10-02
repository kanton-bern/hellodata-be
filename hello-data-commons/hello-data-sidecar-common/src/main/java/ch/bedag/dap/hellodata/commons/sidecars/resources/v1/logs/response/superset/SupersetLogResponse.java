package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.base.BaseSupersetResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupersetLogResponse extends BaseSupersetResponse {
    private List<SupersetLog> result;//NOSONAR
}
