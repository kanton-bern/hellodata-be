package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data;

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCacheUpdate {
    private ModuleType moduleType;
}
