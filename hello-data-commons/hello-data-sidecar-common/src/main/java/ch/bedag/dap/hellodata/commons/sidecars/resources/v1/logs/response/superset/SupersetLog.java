package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupersetLog implements Serializable {
    private SubsystemUser user;
    @JsonProperty("action")
    private String action;
    @JsonProperty("dashboard_id")
    private Integer dashboardId;
    private LocalDateTime dttm;
    @JsonProperty("duration_ms")
    private Integer durationMs;
    private String json;
    private String referrer;
    @JsonProperty("slice_id")
    private Integer sliceId;
    @JsonProperty("user_id")
    private Integer userId;
}
