package ch.bedag.dap.hellodata.portal.metainfo.data;

import java.util.List;

public record SubsystemUserDto(String name,
                               String surname,
                               String email,
                               String username,
                               List<String> roles,
                               String subsystemName) {
}
