package ch.bedag.dap.hellodata.portal.superset.data;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class SupersetQueryDto {

    private UUID id;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;

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
