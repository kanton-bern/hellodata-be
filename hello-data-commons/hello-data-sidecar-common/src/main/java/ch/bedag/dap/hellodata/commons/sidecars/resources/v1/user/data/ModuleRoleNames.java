package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data;

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;

import java.util.List;

public record ModuleRoleNames(ModuleType moduleType, List<String> roleNames) {
}
