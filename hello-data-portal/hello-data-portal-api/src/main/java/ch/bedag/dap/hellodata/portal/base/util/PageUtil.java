package ch.bedag.dap.hellodata.portal.base.util;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Log4j2
@UtilityClass
public class PageUtil {

    public static Pageable createPageable(int page, int size, String sort) {
        return createPageable(page, size, sort, "id", Sort.Direction.ASC);
    }

    public static Pageable createPageable(int page, int size, String sort, String defaultSortField, Sort.Direction defaultDirection) {
        log.info("Creating page {} of size {}", page, size);
        sort = StringUtils.defaultIfEmpty(sort, null);

        Sort sorting = Sort.by(Sort.Direction.ASC, defaultSortField);
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String sortField = sortParams[0];
                Sort.Direction direction = Sort.Direction.fromString(sortParams[1].trim());
                sorting = Sort.by(direction, sortField);
            }
            log.info("Sorting by {} {}", sorting, sort);
        }
        return PageRequest.of(page, size, sorting);
    }
}
