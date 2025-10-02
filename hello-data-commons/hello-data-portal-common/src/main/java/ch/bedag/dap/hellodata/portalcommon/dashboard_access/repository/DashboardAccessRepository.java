package ch.bedag.dap.hellodata.portalcommon.dashboard_access.repository;

import ch.bedag.dap.hellodata.portalcommon.dashboard_access.entity.DashboardAccessEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardAccessRepository extends JpaRepository<DashboardAccessEntity, UUID> {

    Optional<DashboardAccessEntity> findByContextKeyAndDttm(String contextKey, LocalDateTime dttm);

    Page<DashboardAccessEntity> findAllByContextKey(Pageable pageable, String contextKey);

    @Query(value = "SELECT da FROM dashboard_access da WHERE " +
            "da.contextKey = :contextKey " +
            "AND (LOWER(da.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "AND LOWER(da.userFullname) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(CAST(da.dashboardSlug as string)) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(da.dashboardTitle) LIKE LOWER(CONCAT('%', :search, '%'))) ")
    Page<DashboardAccessEntity> findAll(Pageable pageable, @Param("contextKey") String contextKey, @Param("search") String search);

    @Query(value = "SELECT da FROM dashboard_access da WHERE " +
            "LOWER(da.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "AND LOWER(da.userFullname) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(CAST(da.dashboardSlug as string)) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(da.dashboardTitle) LIKE LOWER(CONCAT('%', :search, '%')) ")
    Page<DashboardAccessEntity> findAll(Pageable pageable, @Param("search") String search);

    Optional<DashboardAccessEntity> findFirstByContextKeyOrderByDttmDesc(String contextKey);

}
