package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupersetQueryDatabase implements Serializable {
    @JsonProperty("database_name")
    private String databaseName;
    private Integer id;
}
