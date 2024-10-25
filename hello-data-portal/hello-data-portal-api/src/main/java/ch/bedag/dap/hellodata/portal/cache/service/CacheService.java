package ch.bedag.dap.hellodata.portal.cache.service;

import java.util.function.Supplier;

public interface CacheService {
    <T> void updateCache(String cacheName, Supplier<T> cacheSupplier);
}
