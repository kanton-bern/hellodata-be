package ch.bedag.dap.hellodata.portal.metainfo.data;

import java.util.List;

public record DashboardUsersResultDto(String contextName, String instanceName, List<SubsystemUserDto> users) {
}
