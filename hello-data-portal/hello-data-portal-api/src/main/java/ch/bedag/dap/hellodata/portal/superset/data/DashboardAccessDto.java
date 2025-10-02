package ch.bedag.dap.hellodata.portal.superset.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class DashboardAccessDto {

    @EqualsAndHashCode.Include
    private UUID id;
    private String contextKey;
    private String username;
    private Integer userId;
    private String userFullname;
    private Integer dashboardId;
    private String dashboardTitle;
    private String dashboardSlug;
    private long dttm;
    private String json;
    private String referrer;
    private String contextName;

}
