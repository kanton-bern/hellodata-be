package ch.bedag.dap.hellodata.portal.excel.data;

import ch.bedag.dap.hellodata.portal.user.data.UpdateContextRolesForUserDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BatchUpdateContextRolesForUserDto extends UpdateContextRolesForUserDto {
    private String email;
}
