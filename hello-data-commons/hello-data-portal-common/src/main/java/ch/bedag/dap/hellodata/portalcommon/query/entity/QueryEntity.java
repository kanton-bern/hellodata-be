package ch.bedag.dap.hellodata.portalcommon.query.entity;

import ch.badag.dap.hellodata.commons.basemodel.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * SQL LAB Query Entity gathered from all data domains
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity(name = "query")
public class QueryEntity extends BaseEntity {

    @Column(name = "context_key", nullable = false)
    private String contextKey;

    @Column(name = "subsystem_id", nullable = false)
    private Integer subsystemId;

    @Column(name = "database_name", nullable = false, length = 256)
    private String databaseName;

    @Column(name = "tmp_table_name", length = 256)
    private String tmpTableName;

    @Column(name = "tab_name", length = 256)
    private String tabName;

    @Column(name = "user_fullname", length = 256, nullable = false)
    private String userFullname;

    @Column(name = "username", length = 256)
    private String username;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "schema", length = 256)
    private String schema;

    @Lob
    @Column(name = "sql")
    private String sql;

    @Lob
    @Column(name = "executed_sql")
    private String executedSql;

    @Column(name = "rows")
    private Integer rows;

    @Column(name = "start_time", precision = 20, scale = 6)
    private BigDecimal startTime;

    @Column(name = "changed_on")
    private OffsetDateTime changedOn;

    @Column(name = "end_time", precision = 20, scale = 6)
    private BigDecimal endTime;

    @Lob
    @Column(name = "tracking_url")
    private String trackingUrl;

    @Column(name = "tmp_schema_name", length = 256)
    private String tmpSchemaName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "json", name = "sql_tables")
    private Object sqlTables;

}
