package ch.bedag.dap.hellodata.portal.user.data;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class") // NOSONAR
@Data
public class UserWithBusinessRoleDto extends UserDto {
    private HdRoleName businessDomainRole;
}
