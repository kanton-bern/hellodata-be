package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

public class UploadDashboardsFileException extends RuntimeException {
    public UploadDashboardsFileException(String message) {
        super(message);
    }

    public UploadDashboardsFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
