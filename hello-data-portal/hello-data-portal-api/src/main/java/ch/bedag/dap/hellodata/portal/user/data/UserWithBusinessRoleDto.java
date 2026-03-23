package ch.bedag.dap.hellodata.portal.user.data;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.metainfo.data.DataDomainRoleDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class") // NOSONAR
@Data
public class UserWithBusinessRoleDto extends UserDto {
    private HdRoleName businessDomainRole;
    private List<DataDomainRoleDto> dataDomainRoles;
}
