package ch.bedag.dap.hellodata.portal.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@Log4j2
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final CacheManager cacheManager;

    @Override
    @Transactional
    public <T> void updateCache(String cacheName, Supplier<T> cacheSupplier) {
        LocalDateTime startTime = LocalDateTime.now();
        log.debug("Updating cache {} started", cacheName);
        T result = cacheSupplier.get(); // Call the supplier to get fresh data

        // Update the cache with the new result
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).put(cacheName, result);
            Duration between = Duration.between(startTime, LocalDateTime.now());
            log.info("Cache '{}' updated. Operation took: {}", cacheName, DurationFormatUtils.formatDurationHMS(between.toMillis()));
        }

    }
}
