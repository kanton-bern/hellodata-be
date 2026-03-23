package ch.bedag.dap.hellodata.portal.metainfo.data;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;

public record DataDomainRoleDto(String contextName, String contextKey, HdRoleName role) {
}
