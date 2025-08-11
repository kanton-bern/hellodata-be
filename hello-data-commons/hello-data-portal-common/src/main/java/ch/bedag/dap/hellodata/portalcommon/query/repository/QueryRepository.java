package ch.bedag.dap.hellodata.portalcommon.query.repository;

import ch.bedag.dap.hellodata.portalcommon.query.entity.QueryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryRepository extends JpaRepository<QueryEntity, UUID> {

    Page<QueryEntity> findAllByContextKey(Pageable pageable, String contextKey);

    @Query(value = "SELECT q FROM query q WHERE " +
            "q.contextKey = :contextKey " +
            "AND (LOWER(q.status) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.databaseName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.schema) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(CAST(q.sqlTables as string)) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.userFullname) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(CAST(q.executedSql as string)) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.tabName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<QueryEntity> findAll(Pageable pageable, @Param("contextKey") String contextKey, @Param("search") String search);

    long countAllByContextKey(String contextKey);

    Optional<QueryEntity> findFirstByContextKeyOrderByChangedOnDesc(String contextKey);

}
