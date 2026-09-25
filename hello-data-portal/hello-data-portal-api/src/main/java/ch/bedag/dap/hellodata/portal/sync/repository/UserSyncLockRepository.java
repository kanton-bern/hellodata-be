package ch.bedag.dap.hellodata.portal.sync.repository;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface UserSyncLockRepository extends JpaRepository<UserSyncLockEntity, UUID> {

    /**
     * Atomically moves the status, so only one portal instance wins a transition
     *
     * @return number of changed rows, 0 when the status was not the expected one
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update user_sync_lock l set l.status = :newStatus, l.modifiedDate = :now where l.status = :currentStatus")
    int changeStatus(@Param("currentStatus") UserSyncStatus currentStatus, @Param("newStatus") UserSyncStatus newStatus, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update user_sync_lock l set l.status = :newStatus, l.modifiedDate = :now where l.status = :currentStatus and l.modifiedDate < :olderThan")
    int changeStatusIfOlderThan(@Param("currentStatus") UserSyncStatus currentStatus, @Param("newStatus") UserSyncStatus newStatus,
                                @Param("olderThan") LocalDateTime olderThan, @Param("now") LocalDateTime now);
}
