package ch.bedag.dap.hellodata.portal.sync.repository;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UserSyncLockRepository extends JpaRepository<UserSyncLockEntity, UUID> {

    /**
     * A single update, so requests registered at the same time by different portal instances are not lost
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update user_sync_lock l set l.syncRequestedFirstAt = coalesce(l.syncRequestedFirstAt, :requestedAt), l.syncRequestedLastAt = :requestedAt")
    int registerSyncRequest(@Param("requestedAt") Instant requestedAt);

    /**
     * Clears the requests only if no new one came in since they were read, a newer one stays pending
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update user_sync_lock l set l.syncRequestedFirstAt = null, l.syncRequestedLastAt = null where l.syncRequestedLastAt = :handledLastRequestAt")
    int clearHandledSyncRequests(@Param("handledLastRequestAt") Instant handledLastRequestAt);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update user_sync_lock l set l.lastAutoSyncAt = :syncStartedAt")
    int setLastAutoSyncAt(@Param("syncStartedAt") Instant syncStartedAt);
}
