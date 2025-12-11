package ch.bedag.dap.hellodata.portalcommon.dashboard_access.entity;

import ch.badag.dap.hellodata.commons.basemodel.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * Dashboard Accesses gathered from all data domains
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity(name = "dashboard_access")
public class DashboardAccessEntity extends BaseEntity {
    @Column(name = "context_key", nullable = false)
    private String contextKey;
    @Column(name = "username", length = 256)
    private String username;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "user_fullname", length = 256, nullable = false)
    private String userFullname;
    @Column(name = "dashboard_id", nullable = false)
    private Integer dashboardId;
    @Column(name = "dashboard_title", nullable = false)
    private String dashboardTitle;
    @Column(name = "dashboard_slug", nullable = false)
    private String dashboardSlug;
    @Column(name = "dttm", nullable = false)
    private OffsetDateTime dttm;
    private String json;
    private String referrer;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
