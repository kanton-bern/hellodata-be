package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupersetQuery implements Serializable {
    private SubsystemUser user;
    @JsonProperty("tracking_url")
    private String trackingUrl;
    @JsonProperty("tmp_table_name")
    private String tmpTableName;
    private String status;
    @JsonProperty("start_time")
    private BigDecimal startTime;
    @JsonProperty("end_time")
    private BigDecimal endTime;
    @JsonProperty("sql_tables")
    private Object sqlTables; //NOSONAR
    private String sql;
    private String schema;
    private Integer rows;
    private Integer id;
    @JsonProperty("executed_sql")
    private String executedSql;
    @JsonProperty("changed_on")
    private LocalDateTime changedOn;
    private SupersetQueryDatabase database;
    @JsonProperty("tab_name")
    private String tabName;
}