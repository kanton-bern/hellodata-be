package ch.bedag.dap.hellodata.portal.csv.data;

import java.util.List;

public record CsvUserRole(String email, String businessDomainRole, String context, String dataDomainRole,
                          List<String> supersetRoles) {
}
