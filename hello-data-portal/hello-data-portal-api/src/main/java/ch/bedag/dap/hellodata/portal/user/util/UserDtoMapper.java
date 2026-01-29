package ch.bedag.dap.hellodata.portal.user.util;

import ch.bedag.dap.hellodata.commons.security.Permission;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Log4j2
@UtilityClass
public class UserDtoMapper {

    public static UserDto map(UserEntity userEntity) {
        UserDto userDto = null;
        if (userEntity != null) {
            userDto = new UserDto();
            userDto.setId(userEntity.getId().toString());
            userDto.setEmail(userEntity.getEmail());
            userDto.setUsername(userEntity.getUsername());
            userDto.setEnabled(userEntity.isEnabled());
            userDto.setSuperuser(userEntity.isSuperuser());
            userDto.setInvitationsCount(userEntity.getInvitationsCount());
            userDto.setFirstName(userEntity.getFirstName());
            userDto.setLastName(userEntity.getLastName());
            userDto.setFederated(userEntity.isFederated());
            if (userEntity.getLastAccess() != null) {
                ZonedDateTime zdt = ZonedDateTime.of(userEntity.getLastAccess(), ZoneId.systemDefault());
                userDto.setLastAccess(zdt.toInstant().toEpochMilli());
            }
            if (BooleanUtils.isTrue(userEntity.isSuperuser())) {
                userDto.setPermissions(Arrays.stream(Permission.values()).map(Enum::name).toList());
            } else {
                List<String> portalPermissions = userEntity.getPermissionsFromAllRoles();
                if (portalPermissions != null) {
                    userDto.setPermissions(portalPermissions);
                }
            }
        }
        return userDto;
    }
}
