package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
public abstract class AbstractUserInitializer {

    protected final Keycloak keycloak;
    protected final UserRepository userRepository;

    @Value("${hello-data.auth-server.realm}")
    protected String realmName;

    @NotNull
    protected String createUserInKeycloak(UserRepresentation user) {
        // Save the user
        Response response = keycloak.realm(realmName).users().create(user);
        String userId;
        try (response) {
            HttpStatusCode status = HttpStatusCode.valueOf(response.getStatus());
            if (!status.is2xxSuccessful()) {
                throw new ResponseStatusException(status);
            }
            URI uri = response.getLocation();
            String path = uri.getPath();
            userId = path.substring(path.lastIndexOf('/') + 1);
        }
        return userId;
    }

    protected UserEntity saveUserToDatabase(String userId, String email) {
        //update user
        UserEntity userEntity;
        Optional<UserEntity> resultSearch = userRepository.findById(UUID.fromString(userId));
        if (resultSearch.isPresent()) {
            userEntity = resultSearch.get();
        } else {
            userEntity = new UserEntity();
            userEntity.setId(UUID.fromString(userId));
            userEntity.setEmail(email);
        }
        userRepository.saveAndFlush(userEntity);
        return userRepository.getReferenceById(userEntity.getId());
    }

    protected UserRepresentation generateUser(String username, String firstName, String lastName, String email) {
        // Create a new user
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        return user;
    }
}
