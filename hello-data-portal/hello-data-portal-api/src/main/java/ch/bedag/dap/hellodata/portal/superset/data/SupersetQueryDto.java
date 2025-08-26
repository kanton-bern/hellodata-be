package ch.bedag.dap.hellodata.portal.superset.data;

import ch.bedag.dap.hellodata.portal.base.config.LocalDateTimeToMillisSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SupersetQueryDto {

    private UUID id;
    @JsonSerialize(using = LocalDateTimeToMillisSerializer.class)
    private LocalDateTime createdDate;
    private String createdBy;
    @JsonSerialize(using = LocalDateTimeToMillisSerializer.class)
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
    @JsonSerialize(using = LocalDateTimeToMillisSerializer.class)
    private LocalDateTime changedOn;
    private BigDecimal endTime;
    private String trackingUrl;
    private String tmpSchemaName;
    private Object sqlTables;

}
