package ch.bedag.dap.hellodata.sidecars.sftpgo.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.SftpGoService;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.resource.SftpGoUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DISABLE_USER;

@Log4j2
@Service
@SuppressWarnings("java:S3516")
@RequiredArgsConstructor
public class SftpGoDisableUserConsumer {
    private final SftpGoService sftpgoService;
    private final SftpGoUserResourceProviderService sftpGoUserResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = DISABLE_USER, asyncRun = false)
    public void disableUser(SubsystemUserUpdate subsystemUserUpdate) {
        try {
            log.info("------- Received SFTPGo user disable request {}", subsystemUserUpdate);
            sftpgoService.disableUser(subsystemUserUpdate.getUsername());
            if (subsystemUserUpdate.isSendBackUsersList()) {
                sftpGoUserResourceProviderService.publishUsers();
            }
        } catch (Exception e) {
            log.error("Could not disable user {}", subsystemUserUpdate.getEmail(), e);
        }
    }
}
