package ch.bedag.dap.hellodata.portal.sync.repository;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserSyncLockRepository extends JpaRepository<UserSyncLockEntity, UUID> {

}
