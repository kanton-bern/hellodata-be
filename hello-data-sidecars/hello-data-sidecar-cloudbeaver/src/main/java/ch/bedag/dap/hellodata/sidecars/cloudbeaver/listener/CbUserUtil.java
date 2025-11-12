package ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener;

import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.User;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class CbUserUtil {

    static User generateCbUser(String username, String email, String firstName, String lastName) {
        User dbtDocUser = new User(username, email);
        dbtDocUser.setRoles(new ArrayList<>());
        dbtDocUser.setFirstName(firstName);
        dbtDocUser.setLastName(lastName);
        dbtDocUser.setEnabled(true);
        dbtDocUser.setSuperuser(false);
        return dbtDocUser;
    }

}
