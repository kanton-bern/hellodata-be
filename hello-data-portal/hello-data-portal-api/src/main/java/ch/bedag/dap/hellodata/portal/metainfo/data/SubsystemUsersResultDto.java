package ch.bedag.dap.hellodata.portal.metainfo.data;

import java.util.List;

public record SubsystemUsersResultDto(String instanceName, List<SubsystemUserDto> users) {
}
