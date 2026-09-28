package ch.bedag.dap.hellodata.portal.sync.entity;

import ch.badag.dap.hellodata.commons.basemodel.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

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

    /**
     * First and last users sync request from the sidecars not handled yet, see UsersSyncRequestService
     */
    @Column(name = "sync_requested_first_at")
    private Instant syncRequestedFirstAt;

    @Column(name = "sync_requested_last_at")
    private Instant syncRequestedLastAt;

    @Column(name = "last_auto_sync_at")
    private Instant lastAutoSyncAt;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
