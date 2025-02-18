package ch.bedag.dap.hellodata.portal.user;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("@User email already exists");
    }
}
