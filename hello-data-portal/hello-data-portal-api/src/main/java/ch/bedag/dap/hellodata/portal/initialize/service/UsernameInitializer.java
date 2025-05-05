package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.portal.user.service.KeycloakService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class UsernameInitializer {
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initUsernames() {
        List<UserEntity> allByUsernameIsNull = userRepository.findAllByUsernameIsNull();
        for (UserEntity userEntity : allByUsernameIsNull) {
            log.info("Initializing username for {}", userEntity.getEmail());
            UserRepresentation userRepresentationByEmail = keycloakService.getUserRepresentationByEmail(userEntity.getEmail());
            if (userRepresentationByEmail == null) {
                throw new IllegalStateException("User %s not found in the keycloak!".formatted(userEntity.getEmail()));
            }
            userEntity.setUsername(userRepresentationByEmail.getUsername());
            userRepository.save(userEntity);
        }
    }

}
