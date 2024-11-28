package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsersContextRoleUpdate {
    private List<UserContextRoleUpdate> userContextRoleUpdates;
}
