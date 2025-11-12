package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.portal.superset.data.SupersetQueryDto;
import ch.bedag.dap.hellodata.portalcommon.query.entity.QueryEntity;
import ch.bedag.dap.hellodata.portalcommon.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryRepository queryRepository;

    @Transactional(readOnly = true)
    public Page<SupersetQueryDto> findQueries(String contextKey, Pageable pageable, String search) {
        Page<QueryEntity> allByContextKeyPageable;
        if (StringUtils.isNotBlank(search)) {
            allByContextKeyPageable = queryRepository.findAll(pageable, contextKey, search);
        } else {
            allByContextKeyPageable = queryRepository.findAllByContextKey(pageable, contextKey);
        }
        log.info("Fetched queries {} for contextKey: {} and search: {}", allByContextKeyPageable, contextKey, search);
        return allByContextKeyPageable.map(queryEntity -> {
            SupersetQueryDto dto = new SupersetQueryDto();
            dto.setContextKey(queryEntity.getContextKey());
            dto.setId(queryEntity.getId());
            dto.setCreatedBy(queryEntity.getCreatedBy());
            dto.setCreatedDate(queryEntity.getCreatedDate());
            dto.setModifiedBy(queryEntity.getModifiedBy());
            dto.setModifiedDate(queryEntity.getModifiedDate());
            dto.setRows(queryEntity.getRows());
            dto.setStartTime(queryEntity.getStartTime());
            dto.setEndTime(queryEntity.getEndTime());
            dto.setTrackingUrl(queryEntity.getTrackingUrl());
            dto.setSchema(queryEntity.getSchema());
            dto.setTmpSchemaName(queryEntity.getTmpSchemaName());
            dto.setExecutedSql(queryEntity.getExecutedSql());
            dto.setSql(queryEntity.getSql());
            dto.setSqlTables(queryEntity.getSqlTables());
            dto.setChangedOn(queryEntity.getChangedOn().toInstant().toEpochMilli());
            dto.setUsername(queryEntity.getUsername());
            dto.setUserFullname(queryEntity.getUserFullname());
            dto.setStatus(queryEntity.getStatus());
            dto.setTabName(queryEntity.getTabName());
            dto.setTmpTableName(queryEntity.getTmpTableName());
            dto.setDatabaseName(queryEntity.getDatabaseName());
            dto.setSubsystemId(queryEntity.getSubsystemId());
            return dto;
        });
    }

}
