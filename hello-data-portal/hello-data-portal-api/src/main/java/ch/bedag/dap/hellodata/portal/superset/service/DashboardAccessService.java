package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.portal.superset.data.DashboardAccessDto;
import ch.bedag.dap.hellodata.portalcommon.dashboard_access.entity.DashboardAccessEntity;
import ch.bedag.dap.hellodata.portalcommon.dashboard_access.repository.DashboardAccessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardAccessService {

    private final DashboardAccessRepository dashboardAccessRepository;
    private final HdContextRepository hdContextRepository;

    @Transactional(readOnly = true)
    public Page<DashboardAccessDto> findDashboardAccess(String contextKey, Pageable pageable, String search) {
        Page<DashboardAccessEntity> allByContextKeyPageable;
        if (StringUtils.isNotBlank(contextKey)) {
            allByContextKeyPageable = fetchByContextKey(contextKey, pageable, search);
        } else {
            allByContextKeyPageable = fetchAll(pageable, search);
        }
        List<HdContextEntity> allContexts = hdContextRepository.findAll();

        log.debug("Fetched dashboard access list {} for contextKey: {} and search: {}", allByContextKeyPageable, contextKey, search);
        return allByContextKeyPageable.map(dashboardAccessEntity -> {
            DashboardAccessDto dto = new DashboardAccessDto();
            dto.setContextKey(dashboardAccessEntity.getContextKey());
            dto.setId(dashboardAccessEntity.getId());
            dto.setDttm(dashboardAccessEntity.getDttm().toInstant(ZoneOffset.UTC).toEpochMilli());
            dto.setUsername(dashboardAccessEntity.getUsername());
            dto.setUserFullname(dashboardAccessEntity.getUserFullname());
            dto.setUserId(dashboardAccessEntity.getUserId());

            dto.setDashboardId(dashboardAccessEntity.getDashboardId());
            dto.setDashboardSlug(dashboardAccessEntity.getDashboardSlug());
            dto.setDashboardTitle(dashboardAccessEntity.getDashboardTitle());
            dto.setJson(dashboardAccessEntity.getJson());
            dto.setReferrer(dashboardAccessEntity.getReferrer());
            HdContextEntity hdContextEntity = allContexts.stream().filter(ctx -> ctx.getContextKey().equals(dto.getContextKey())).findFirst().orElse(null);
            if (hdContextEntity != null) {
                dto.setContextName(hdContextEntity.getName());
            }
            return dto;
        });
    }

    private Page<DashboardAccessEntity> fetchAll(Pageable pageable, String search) {
        Page<DashboardAccessEntity> allByContextKeyPageable;
        if (StringUtils.isNotBlank(search)) {
            allByContextKeyPageable = dashboardAccessRepository.findAll(pageable, search);
        } else {
            allByContextKeyPageable = dashboardAccessRepository.findAll(pageable);
        }
        return allByContextKeyPageable;
    }

    private Page<DashboardAccessEntity> fetchByContextKey(String contextKey, Pageable pageable, String search) {
        Page<DashboardAccessEntity> allByContextKeyPageable;
        if (StringUtils.isNotBlank(search)) {
            allByContextKeyPageable = dashboardAccessRepository.findAll(pageable, contextKey, search);
        } else {
            allByContextKeyPageable = dashboardAccessRepository.findAllByContextKey(pageable, contextKey);
        }
        return allByContextKeyPageable;
    }
}
