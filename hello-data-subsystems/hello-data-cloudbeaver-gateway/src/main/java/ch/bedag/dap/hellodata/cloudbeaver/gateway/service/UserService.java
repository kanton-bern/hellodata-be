package ch.bedag.dap.hellodata.cloudbeaver.gateway.service;

import ch.bedag.dap.hellodata.cloudbeaver.gateway.entities.User;
import ch.bedag.dap.hellodata.cloudbeaver.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public User findOneWithPermissionsByEmail(String email) {
        try {
            return userRepository.findOneWithPermissionsByEmail(email).toFuture().get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Could not fetch user from the DB", e);
        }
    }
}
