package ch.bedag.dap.hellodata.portal.sync.controller;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import ch.bedag.dap.hellodata.portal.sync.service.UsersSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-sync")
public class UsersSyncController {

    private final UsersSyncService usersSyncService;

    @GetMapping("/start")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public UserSyncStatus syncUsers() {
        return usersSyncService.startSynchronization();
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public UserSyncStatus getSyncStatus() {
        return usersSyncService.getSyncStatus();
    }
}
