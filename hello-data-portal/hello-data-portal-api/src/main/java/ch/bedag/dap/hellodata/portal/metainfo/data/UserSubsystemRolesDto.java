package ch.bedag.dap.hellodata.portal.metainfo.data;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import java.util.List;
import java.util.Map;

public record UserSubsystemRolesDto(
    String email,
    String firstName,
    String lastName,
    boolean enabled,
    HdRoleName businessDomainRole,
    List<DataDomainRoleDto> dataDomainRoles,
    Map<String, List<String>> subsystemRoles
) {
}
