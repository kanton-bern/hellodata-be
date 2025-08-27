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
        return allByContextKeyPageable.map(userEntity -> {
            SupersetQueryDto dto = new SupersetQueryDto();
            dto.setContextKey(userEntity.getContextKey());
            dto.setId(userEntity.getId());
            dto.setCreatedBy(userEntity.getCreatedBy());
            dto.setCreatedDate(userEntity.getCreatedDate());
            dto.setModifiedBy(userEntity.getModifiedBy());
            dto.setModifiedDate(userEntity.getModifiedDate());
            dto.setRows(userEntity.getRows());
            dto.setStartTime(userEntity.getStartTime());
            dto.setEndTime(userEntity.getEndTime());
            dto.setTrackingUrl(userEntity.getTrackingUrl());
            dto.setSchema(userEntity.getSchema());
            dto.setTmpSchemaName(userEntity.getTmpSchemaName());
            dto.setExecutedSql(userEntity.getExecutedSql());
            dto.setSql(userEntity.getSql());
            dto.setSqlTables(userEntity.getSqlTables());
            dto.setChangedOn(userEntity.getChangedOn().toInstant().toEpochMilli());
            dto.setUsername(userEntity.getUsername());
            dto.setUserFullname(userEntity.getUserFullname());
            dto.setStatus(userEntity.getStatus());
            dto.setTabName(userEntity.getTabName());
            dto.setTmpTableName(userEntity.getTmpTableName());
            dto.setDatabaseName(userEntity.getDatabaseName());
            dto.setSubsystemId(userEntity.getSubsystemId());
            return dto;
        });
    }

}
