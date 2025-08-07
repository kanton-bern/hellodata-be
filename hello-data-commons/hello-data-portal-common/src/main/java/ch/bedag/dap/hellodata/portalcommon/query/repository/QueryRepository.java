package ch.bedag.dap.hellodata.portalcommon.query.repository;

import ch.bedag.dap.hellodata.portalcommon.query.entity.QueryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryRepository extends JpaRepository<QueryEntity, UUID> {

    Page<QueryEntity> findAllByContextKey(Pageable pageable, String contextKey);

    long countAllByContextKey(String contextKey);

    Optional<QueryEntity> findFirstByContextKeyOrderByChangedOnDesc(String contextKey);

}
