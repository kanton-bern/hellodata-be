/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.portal.csv.service.CsvParserService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.UserAlreadyExistsException;
import ch.bedag.dap.hellodata.portal.user.data.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class BatchUsersInvitationService {

    private final CsvParserService csvParserService;
    private final UserService userService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final RoleService roleService;
    private final BatchUsersCustomLogger batchUsersCustomLogger;

    private final String batchUsersFileLocation;

    public BatchUsersInvitationService(CsvParserService csvParserService,
                                       UserService userService,
                                       MetaInfoResourceService metaInfoResourceService,
                                       RoleService roleService,
                                       BatchUsersCustomLogger batchUsersCustomLogger,
                                       @Value("${hello-data.batch-users-file.location}") String batchUsersFileLocation) {
        this.csvParserService = csvParserService;
        this.userService = userService;
        this.batchUsersFileLocation = batchUsersFileLocation;
        this.metaInfoResourceService = metaInfoResourceService;
        this.roleService = roleService;
        this.batchUsersCustomLogger = batchUsersCustomLogger;
    }

    @Scheduled(fixedDelayString = "${hello-data.batch-users-file.scan-interval-seconds}", timeUnit = TimeUnit.SECONDS)
    public void inviteOrUpdateUsers() {
        try {
            ContextsDto availableContexts = userService.getAvailableContexts();
            List<BatchUpdateContextRolesForUserDto> users = fetchDataFromFile(true, availableContexts);
            String userEmails = users.stream()
                    .map(BatchUpdateContextRolesForUserDto::getEmail)
                    .collect(Collectors.joining(", "));

            if (!CollectionUtils.isEmpty(users)) {
                String importUsersMsg = "Inviting/updating %s users: %s".formatted(users.size(), userEmails);
                log.info(importUsersMsg);
                batchUsersCustomLogger.logMessage(importUsersMsg);
            } else {
                log.debug("No users were invited/updated");
                return;
            }
            List<RoleDto> allRoles = roleService.getAll();
            List<UserDto> allUsers = userService.getAllUsers();

            createOrUpdateUsers(users, allUsers, allRoles, availableContexts);
            batchUsersCustomLogger.logMessage("Processed %s users successfully%n".formatted(users.size()));
        } catch (Exception e) {
            log.error("Error inviting/updating users", e);
            batchUsersCustomLogger.logMessage("Error inviting users: %s %n".formatted(e.getMessage()));
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void createOrUpdateUsers(List<BatchUpdateContextRolesForUserDto> users, List<UserDto> allUsers, List<RoleDto> allRoles, ContextsDto availableContexts) throws InterruptedException {
        int i = 0;
        for (BatchUpdateContextRolesForUserDto user : users) {
            batchUsersCustomLogger.logMessage(String.format("Processing user %s", user.getEmail()));
            List<AdUserDto> usersSearched = this.userService.searchUser(user.getEmail());
            log.info("Found {} user(s) by email {}: {}", usersSearched.size(), user.getEmail(), usersSearched);
            Optional<AdUserDto> any = usersSearched.stream().findAny();
            log.info("Found any? {}", any.isPresent());
            Optional<AdUserDto> firstAD = usersSearched.stream().filter(adUserDto -> adUserDto.getOrigin() == AdUserOrigin.LDAP).findFirst();
            AdUserDto adUserDto = firstAD.orElseGet(() -> any.orElseThrow(() -> new NoSuchElementException("User not found: " + user.getEmail())));
            String userId;
            try {
                userId = userService.createUser(adUserDto.getEmail(), adUserDto.getFirstName(), adUserDto.getLastName(), adUserDto.getOrigin());
                Thread.sleep(1000L); //wait for it to push to subsystems before proceeding to set context roles
                batchUsersCustomLogger.logMessage(String.format("Created user %s", user.getEmail()));
            } catch (UserAlreadyExistsException e) {
                String errMsg = "User %s already exists, updating roles...".formatted(adUserDto.getEmail());
                log.info(errMsg);
                batchUsersCustomLogger.logMessage(errMsg);
                userId = allUsers.stream().filter(userDto -> userDto.getEmail().equalsIgnoreCase(adUserDto.getEmail())).findFirst().get().getId();
            }
            insertFullBusinessDomainRole(user, allRoles);
            insertFullContextRoles(user, allRoles, availableContexts);
            user.setSelectedDashboardsForUser(new HashMap<>()); // prevent NPE
            boolean isLast = i == users.size() - 1;
            userService.updateContextRolesForUser(UUID.fromString(userId), user, isLast);
            i++;
        }
    }

    /**
     * Inserts full context roles to the selection from the file. The selection from the file has only role name and context key.
     *
     * @param user
     * @param allRoles
     * @param availableContexts
     */
    private void insertFullContextRoles(BatchUpdateContextRolesForUserDto user, List<RoleDto> allRoles, ContextsDto availableContexts) {
        List<UserContextRoleDto> dataDomainRoles = user.getDataDomainRoles().stream().map(dataDomainRole -> {
            RoleDto role = allRoles.stream()
                    .filter(roleDto -> roleDto.getName().equalsIgnoreCase(dataDomainRole.getRole().getName()))
                    .findAny().orElseThrow(() -> new RuntimeException("Role not found"));
            dataDomainRole.setRole(role);
            dataDomainRole.setContext(availableContexts.getContexts().stream()
                    .filter(contextDto -> contextDto.getContextKey().equalsIgnoreCase(dataDomainRole.getContext().getContextKey()))
                    .findAny().orElseThrow(() -> new RuntimeException("Context not found")));
            return dataDomainRole;
        }).collect(Collectors.toList());
        user.setDataDomainRoles(dataDomainRoles);
    }

    /**
     * Inserts full business domain role to the selection from the file. The selection from the file has only name and context.
     *
     * @param user
     * @param allRoles
     */
    private void insertFullBusinessDomainRole(BatchUpdateContextRolesForUserDto user, List<RoleDto> allRoles) {
        RoleDto businessDomainRole = allRoles.stream()
                .filter(roleDto -> roleDto.getName().equalsIgnoreCase(user.getBusinessDomainRole().getName()))
                .findAny().orElseThrow(() -> new RuntimeException("Role not found"));
        user.setBusinessDomainRole(businessDomainRole);
    }

    private void deleteFile(File file) {
        if (file.delete()) { //NOSONAR
            log.info("Batch users file successfully deleted: {}", file.getAbsolutePath());
        } else {
            log.error("Failed to delete batch users file: {}", file.getAbsolutePath());
        }
    }

    private void verifyContextKeys(List<BatchUpdateContextRolesForUserDto> users, Set<String> availableDataDomainKeys) {
        for (BatchUpdateContextRolesForUserDto user : users) {
            for (UserContextRoleDto dataDomainRole : user.getDataDomainRoles()) {
                if (!availableDataDomainKeys.contains(dataDomainRole.getContext().getContextKey())) {
                    throw new IllegalArgumentException("Context key not found: " + dataDomainRole.getContext().getContextKey());
                }
            }
        }
    }

    private Set<String> getAvailableDataDomainKeys(ContextsDto availableContexts) {
        return availableContexts.getContexts().stream()
                .filter(contextDto -> contextDto.getType() == HdContextType.DATA_DOMAIN)
                .map(ContextDto::getContextKey)
                .collect(Collectors.toSet());
    }

    private Map<String, List<String>> getDataDomainKeyToExistingSupersetRolesMap() {
        return metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES).stream()
                .collect(Collectors.toMap(MetaInfoResourceEntity::getContextKey,
                        metaInfoResourceEntity -> ((RoleResource) metaInfoResourceEntity.getMetainfo()).getData().stream()
                                .map(RolePermissions::name).toList()));
    }

    private void verifySupersetRoles(List<BatchUpdateContextRolesForUserDto> users, Map<String, List<String>> dataDomainKeyToExistingSupersetRolesMap) {
        for (BatchUpdateContextRolesForUserDto user : users) {
            verifySupersetRoleExists(dataDomainKeyToExistingSupersetRolesMap, user);
        }
    }

    private static void verifySupersetRoleExists(Map<String, List<String>> dataDomainKeyToExistingSupersetRolesMap, BatchUpdateContextRolesForUserDto user) {
        user.getContextToModuleRoleNamesMap().forEach((contextKey, moduleRoleNamesList) -> {
            List<String> existingSupersetRoles = dataDomainKeyToExistingSupersetRolesMap.get(contextKey);
            if (existingSupersetRoles != null) {
                moduleRoleNamesList.stream()
                        .filter(moduleRoleNames -> moduleRoleNames.moduleType() == ModuleType.SUPERSET)
                        .findFirst()
                        .ifPresent(supersetModule -> {
                            List<String> proposedRoleNamesFromFile = supersetModule.roleNames();
                            if (!new HashSet<>(existingSupersetRoles).containsAll(proposedRoleNamesFromFile)) {
                                throw new IllegalArgumentException("The following role(s) is not present in the Superset %s, please check it: %s".formatted(contextKey, proposedRoleNamesFromFile));
                            }
                        });
            }
        });
    }

    List<BatchUpdateContextRolesForUserDto> fetchDataFromFile(boolean removeFilesAfterFetch, ContextsDto availableContexts) {
        File directory = new File(batchUsersFileLocation);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("Batch users directory does not exist or is not a directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.debug("No .csv files found in directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        Map<String, List<String>> dataDomainKeyToExistingSupersetRolesMap = getDataDomainKeyToExistingSupersetRolesMap();

        Set<String> availableDataDomainKeys = getAvailableDataDomainKeys(availableContexts);
        List<BatchUpdateContextRolesForUserDto> allUsers = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                log.debug("File {} content \n{}", file.getAbsolutePath(), FileUtils.readFileToString(file, StandardCharsets.UTF_8));
                List<BatchUpdateContextRolesForUserDto> users = csvParserService.transform(fis);
                log.debug("Batch users file successfully read: {}", users);
                verifyContextKeys(users, availableDataDomainKeys);
                verifySupersetRoles(users, dataDomainKeyToExistingSupersetRolesMap);
                allUsers.addAll(users);
                if (removeFilesAfterFetch) {
                    deleteFile(file);
                }
            } catch (IOException e) {
                log.error("Error processing batch users file: {}", file.getAbsolutePath(), e);
                throw new RuntimeException("Error processing batch users file", e); //NOSONAR
            }
        }
        return allUsers;
    }
}
