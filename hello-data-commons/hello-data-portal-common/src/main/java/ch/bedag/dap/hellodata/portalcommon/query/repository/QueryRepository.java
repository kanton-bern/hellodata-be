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

    @Query(nativeQuery = true, value = "SELECT q FROM query q WHERE " +
            "q.context_key = :contextKey " +
            "AND (LOWER(q.status) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.changed_on) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.database_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.schema) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.sql_tables) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.user_fullname) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.executed_sql) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.tab_name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<QueryEntity> findAll(Pageable pageable, @Param("search") String search, @Param("contextKey") String contextKey);

    long countAllByContextKey(String contextKey);

    Optional<QueryEntity> findFirstByContextKeyOrderByChangedOnDesc(String contextKey);

}
