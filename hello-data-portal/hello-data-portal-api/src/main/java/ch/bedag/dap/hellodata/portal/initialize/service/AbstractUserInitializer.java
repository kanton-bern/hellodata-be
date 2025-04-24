package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
public abstract class AbstractUserInitializer {

    protected final Keycloak keycloak;
    protected final UserRepository userRepository;

    @Value("${hello-data.auth-server.realm}")
    protected String realmName;

    @NotNull
    protected String createUserInKeycloak(UserRepresentation user) {
        // Check if the user already exists
        log.info("Check if user exists username: {}, email: {}", user.getUsername(), user.getEmail());
        UsersResource users = keycloak.realm(realmName).users();
        List<UserRepresentation> searchByUsername = users.search(user.getUsername());
        for (UserRepresentation u : searchByUsername) {
            if (u.getUsername().equalsIgnoreCase(user.getUsername()) && u.getEmail().equalsIgnoreCase(user.getEmail())) {
                log.debug("Found user with username: {} and email: {}", u.getUsername(), u.getEmail());
                return u.getId();
            }
        }
        // Save the user
        log.debug("Creating user: {}", user);
        Response response = users.create(user);
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

    protected UserEntity saveUserToDatabase(String userId, String email, String firstName, String lastName, String username) {
        //update user
        UserEntity userEntity;
        Optional<UserEntity> resultSearch = userRepository.findById(UUID.fromString(userId));
        if (resultSearch.isPresent()) {
            userEntity = resultSearch.get();
        } else {
            userEntity = new UserEntity();
            userEntity.setId(UUID.fromString(userId));
            userEntity.setEmail(email);
            userEntity.setFirstName(firstName);
            userEntity.setLastName(lastName);
            userEntity.setUsername(username);
        }
        userEntity.setEnabled(true);
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
