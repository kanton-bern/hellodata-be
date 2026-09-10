package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Provisions the shared DWH group role once the application is up. If the DWH is unreachable at startup the
 * error is only logged - the shared role is re-ensured on the first temporary user creation - so a temporarily
 * unavailable DWH never prevents the sidecar from starting.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class DwhSharedRoleInitializer implements ApplicationRunner {

    private final TemporaryUserService temporaryUserService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            temporaryUserService.ensureSharedRole();
            log.info("Ensured DWH shared role for temporary users");
        } catch (Exception e) {
            log.error("Could not ensure DWH shared role at startup; it will be retried on first temporary user creation", e);
        }
    }
}
