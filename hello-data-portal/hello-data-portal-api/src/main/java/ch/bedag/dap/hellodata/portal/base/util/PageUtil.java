package ch.bedag.dap.hellodata.portal.base.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class PageUtil {

    public static Pageable createPageable(int page, int size, String sort, String search) {
        sort = StringUtils.defaultIfEmpty(sort, null);
        search = StringUtils.defaultIfEmpty(search, null);

        Sort sorting = Sort.by(Sort.Direction.ASC, "id");
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String sortField = sortParams[0];
                Sort.Direction direction = Sort.Direction.fromString(sortParams[1].trim());
                sorting = Sort.by(direction, sortField);
            }
        }
        return PageRequest.of(page, size, sorting);
    }
}
