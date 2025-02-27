package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@AllArgsConstructor
public class UserRoleSyncService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final HdContextRepository contextRepository;
    private final UserService userService;

    /**
     * In case of another data domain has been plugged in, add missing context roles to users
     */
    @Transactional
    @Scheduled(fixedDelayString = "${hello-data.check-user-context-roles-in-minutes}", timeUnit = TimeUnit.MINUTES)
    public void checkUserRolesSync() {
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        log.debug("Available data domains: {}", dataDomains);
        List<String> businessDomains =
                contextRepository.findAllByTypeIn(List.of(HdContextType.BUSINESS_DOMAIN)).stream().map(userContextRole -> userContextRole.getContextKey()).toList();
        log.debug("Available business domains: {}", businessDomains);
        List<String> dataDomainKeys = dataDomains.stream().map(dd -> dd.getContextKey()).toList();
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity userEntity : users) {
            checkUserContextRoleExistence(userEntity, dataDomainKeys, businessDomains);
        }
    }

    /**
     * Check if user has all data domain roles, if not add a default one (DATA_DOMAIN_ADMIN for admins, NONE to others)
     *
     * @param userEntity
     * @param dataDomainKeys
     * @param businessDomains
     */
    private void checkUserContextRoleExistence(UserEntity userEntity, List<String> dataDomainKeys, List<String> businessDomains) {
        Set<UserContextRoleEntity> userContextRoles = userEntity.getContextRoles();
        List<String> contextRoleKeys = userContextRoles.stream().map(cr -> cr.getContextKey()).toList();
        List<String> dataDomainKeysNotFoundInUserRole = fetchElementsNotInList(dataDomainKeys, contextRoleKeys);
        for (String dataDomainKeyNotFoundInUserRole : dataDomainKeysNotFoundInUserRole) {
            log.debug("User {} seems to not have a data domain role for the following key {}, will add a default one", userEntity.getEmail(), dataDomainKeyNotFoundInUserRole);
            addMissingContextRole(userEntity, businessDomains, dataDomainKeyNotFoundInUserRole, userContextRoles);
        }
    }

    private void addMissingContextRole(UserEntity userEntity, List<String> businessDomains, String dataDomainKeyNotFoundInUserRole, Set<UserContextRoleEntity> userContextRoles) {
        Optional<UserContextRoleEntity> businessDomainContextRoleFound =
                userContextRoles.stream().filter(userContextRole -> businessDomains.contains(userContextRole.getContextKey())).findFirst();
        if (businessDomainContextRoleFound.isPresent()) {
            UserContextRoleEntity businessDomainContextRole = businessDomainContextRoleFound.get();
            HdRoleName businessDomainContextRoleName = businessDomainContextRole.getRole().getName();
            if (businessDomainContextRoleName == HdRoleName.BUSINESS_DOMAIN_ADMIN || businessDomainContextRoleName == HdRoleName.HELLODATA_ADMIN) {
                log.debug("User {} is an admin, setting missing role to DATA_DOMAIN_ADMIN", userEntity.getEmail());
                roleService.addContextRoleToUser(userEntity, dataDomainKeyNotFoundInUserRole, HdRoleName.DATA_DOMAIN_ADMIN);
            } else {
                log.debug("User {} is not an admin, setting missing role to NONE", userEntity.getEmail());
                roleService.addContextRoleToUser(userEntity, dataDomainKeyNotFoundInUserRole, HdRoleName.NONE);
            }
            userService.synchronizeContextRolesWithSubsystems(userEntity, new HashMap<>());
        }
    }

    /**
     * Fetch missing elements from the sourceList that are not found in the checkList
     *
     * @param sourceList
     * @param checkList
     * @return
     */
    private List<String> fetchElementsNotInList(List<String> sourceList, List<String> checkList) {
        List<String> notFoundElements = new ArrayList<>();
        for (String element : sourceList) {
            if (!checkList.contains(element)) {
                notFoundElements.add(element);
            }
        }
        return notFoundElements;
    }
}
