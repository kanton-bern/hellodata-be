package ch.bedag.dap.hellodata.jupyterhub.gateway.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "portal_role")
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class PortalRole implements Persistable<UUID> {

    @Id
    private UUID id;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;

    @Basic(fetch = FetchType.EAGER)
    @jakarta.persistence.Column(columnDefinition = "json", name = "permissions")
    private Permissions permissions;

    @Override
    public boolean isNew() {
        return null == this.getId();
    }
}
