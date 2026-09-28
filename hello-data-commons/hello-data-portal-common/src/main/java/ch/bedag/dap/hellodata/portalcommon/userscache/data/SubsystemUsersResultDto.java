package ch.bedag.dap.hellodata.portalcommon.userscache.data;

import java.util.List;

public record SubsystemUsersResultDto(String instanceName, List<SubsystemUserDto> users) {
}
