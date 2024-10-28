package ch.bedag.dap.hellodata.portal.event.service;

import ch.bedag.dap.hellodata.portal.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class EventService {

    private final UserService userService;

//    @EventListener
//    public void handleContextStart(UpdatePortalUsersCacheEvent updatePortalUsersCacheEvent) {
//        log.info("[Cache] Started Refreshing cache for portal users");
//        LocalDateTime startTime = LocalDateTime.now();
//        userService.getAllUsersInternal();
//        Duration between = Duration.between(startTime, LocalDateTime.now());
//        log.info("[Cache] Completed refreshing cache for portal users. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
//    }
}
