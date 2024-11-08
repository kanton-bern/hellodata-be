package ch.bedag.dap.hellodata.sidecars.superset.client.exception;

public class BadReturnedValueException extends RuntimeException {
    public BadReturnedValueException(String message) {
        super(message);
    }
}
