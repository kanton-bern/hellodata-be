package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.base.BaseSupersetResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupersetQueryResponse extends BaseSupersetResponse {
    private List<SupersetQuery> result;//NOSONAR
}
