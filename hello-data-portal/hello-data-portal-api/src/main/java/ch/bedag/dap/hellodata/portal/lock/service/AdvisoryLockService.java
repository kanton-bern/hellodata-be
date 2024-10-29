package ch.bedag.dap.hellodata.portal.lock.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvisoryLockService {

    private static final String ADVISORY_UNLOCK_QUERY_PART = "SELECT pg_advisory_unlock(?)";
    private static final String ADVISORY_LOCK_QUERY = "SELECT pg_try_advisory_lock(?)";

    private final JdbcTemplate jdbcTemplate;

    public AdvisoryLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean acquireLock(long lockId) {
        return jdbcTemplate.queryForObject(ADVISORY_LOCK_QUERY, Boolean.class, lockId);
    }

    @Transactional
    public boolean releaseStaleLock(long lockId) {
        return jdbcTemplate.queryForObject(ADVISORY_UNLOCK_QUERY_PART, Boolean.class, lockId);
    }
}
