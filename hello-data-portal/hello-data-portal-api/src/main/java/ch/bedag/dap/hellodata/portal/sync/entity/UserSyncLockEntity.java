package ch.bedag.dap.hellodata.portal.sync.entity;

import ch.badag.dap.hellodata.commons.basemodel.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity(name = "user_sync_lock")
@Table(name = "user_sync_lock")
public class UserSyncLockEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserSyncStatus status;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
