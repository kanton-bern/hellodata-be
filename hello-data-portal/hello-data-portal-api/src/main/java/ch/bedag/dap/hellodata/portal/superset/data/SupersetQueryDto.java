package ch.bedag.dap.hellodata.portal.superset.data;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class SupersetQueryDto {
    private String contextKey;
    private Integer subsystemId;
    private String databaseName;
    private String tmpTableName;
    private String tabName;
    private String sqlEditorId;
    private String userFullname;
    private String username;
    private String status;
    private String schema;
    private String sql;
    private String executedSql;
    private Integer rows;
    private BigDecimal startTime;
    private OffsetDateTime changedOn;
    private BigDecimal endTime;
    private String trackingUrl;
    private String tmpSchemaName;
    private Object sqlTables;

}
