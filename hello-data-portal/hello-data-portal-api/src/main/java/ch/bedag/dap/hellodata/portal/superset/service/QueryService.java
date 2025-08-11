package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.portal.superset.data.SupersetQueryDto;
import ch.bedag.dap.hellodata.portalcommon.query.entity.QueryEntity;
import ch.bedag.dap.hellodata.portalcommon.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryRepository queryRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public Page<SupersetQueryDto> fetchQueries(String contextKey, Pageable pageable, String search) {
        Page<QueryEntity> allByContextKeyPageable;
        if (StringUtils.isNotBlank(search)) {
            allByContextKeyPageable = queryRepository.findAll(pageable, search, contextKey);
        } else {
            allByContextKeyPageable = queryRepository.findAllByContextKey(pageable, contextKey);
        }
        log.info("Fetched queries {} for contextKey: {} and search: {}", allByContextKeyPageable, contextKey, search);
        return allByContextKeyPageable.map(userEntity -> modelMapper.map(userEntity, SupersetQueryDto.class));
    }

}
