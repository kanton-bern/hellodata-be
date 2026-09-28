package ch.bedag.dap.hellodata.portalcommon.userscache.data;

import java.util.List;

public record DashboardUsersResultDto(String contextName, String instanceName, List<SubsystemUserDto> users) {
}
