package ch.bedag.dap.hellodata.sidecars.sftpgo.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserDelete;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.SftpGoService;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.resource.SftpGoUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DELETE_USER;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DISABLE_USER;

@Log4j2
@Service
@SuppressWarnings("java:S3516")
@RequiredArgsConstructor
public class SftpGoDeleteUserConsumer {
    private final SftpGoService sftpgoService;
    private final SftpGoUserResourceProviderService sftpGoUserResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = DELETE_USER, asyncRun = false)
    public void deleteUser(SubsystemUserDelete subsystemUserDelete) {
        try {
            log.info("------- Received SFTPGo user delete request {}", subsystemUserDelete);
            sftpgoService.deleteUser(subsystemUserDelete.getUsername());
        } catch (Exception e) {
            log.error("Could not delete user {}", subsystemUserDelete.getEmail(), e);
        }
    }
}
