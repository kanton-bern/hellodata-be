package ch.bedag.dap.hellodata.portal.sync.entity;

public enum UserSyncStatus {
    /**
     * Synchronization requested, waiting to be picked up
     */
    STARTED,
    /**
     * Synchronization is running
     */
    RUNNING,
    /**
     * Synchronization is running and another one was requested meanwhile, it starts once the running one finishes
     */
    RERUN_REQUESTED,
    COMPLETED
}
