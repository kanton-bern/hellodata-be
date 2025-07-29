package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupersetDatabase implements Serializable {
    private int id;
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("database_name")
    private String databaseName;
    @JsonProperty("backend")
    private String backend;
    @JsonProperty("changed_by")
    private SubsystemUser changedBy;
    @JsonProperty("changed_by_name")
    private String changedByName;
    @JsonProperty("changed_by_url")
    private String changedByUrl;
    @JsonProperty("changed_on_delta_humanized")
    private String changedOnDeltaHumanized;
    @JsonProperty("changed_on_utc")
    private String changedOnUtc;
    @JsonProperty("created_by")
    private SubsystemUser createdBy;
    @JsonProperty("created_on_delta_humanized")
    private String createdOnDeltaHumanized;
}
