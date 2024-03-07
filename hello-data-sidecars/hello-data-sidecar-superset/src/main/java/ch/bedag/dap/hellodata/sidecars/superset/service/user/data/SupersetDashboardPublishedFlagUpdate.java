package ch.bedag.dap.hellodata.sidecars.superset.service.user.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupersetDashboardPublishedFlagUpdate {
    private boolean published;
}
