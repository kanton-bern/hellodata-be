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

import ch.bedag.dap.hellodata.portal.csv.service.CsvParserService;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.AdUserDto;
import ch.bedag.dap.hellodata.portal.user.data.BatchUpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextsDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class BatchUsersInvitationService {
    private static final String LOCAL_AD_REGEX = "\\b\\w+-\\d+\\b";

    private final CsvParserService csvParserService;
    private final UserService userService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final RoleService roleService;

    private final String batchUsersFileLocation;

    public BatchUsersInvitationService(CsvParserService csvParserService,
                                       UserService userService, MetaInfoResourceService metaInfoResourceService, RoleService roleService,
                                       @Value("${hello-data.batch-users-file.location}") String batchUsersFileLocation) {
        this.csvParserService = csvParserService;
        this.userService = userService;
        this.batchUsersFileLocation = batchUsersFileLocation;
        this.metaInfoResourceService = metaInfoResourceService;
        this.roleService = roleService;
    }

    @Scheduled(fixedDelayString = "${hello-data.batch-users-file.scan-interval-seconds}", timeUnit = TimeUnit.SECONDS)
    public void inviteUsers() throws InterruptedException {
        List<BatchUpdateContextRolesForUserDto> users = fetchUsersFile(true);
        String userEmails = users.stream()
                .map(BatchUpdateContextRolesForUserDto::getEmail)
                .collect(Collectors.joining(", "));
        log.info("Inviting {} users: {}", users.size(), userEmails);

        List<RoleDto> allRoles = roleService.getAll();
        ContextsDto availableContexts = userService.getAvailableContexts();

        for (BatchUpdateContextRolesForUserDto user : users) {
            Optional<AdUserDto> any = this.userService.searchUser(user.getEmail()).stream().findAny();
            Optional<AdUserDto> firstAD = this.userService.searchUser(user.getEmail()).stream().filter(adUserDto -> !adUserDto.getFirstName().matches(LOCAL_AD_REGEX)).findFirst();
            AdUserDto adUserDto = firstAD.orElseGet(any::orElseThrow);
            String userId = userService.createUser(adUserDto.getEmail(), adUserDto.getFirstName(), adUserDto.getLastName());
            insertFullBusinessDomainRole(user, allRoles);
            insertFullContextRoles(user, allRoles, availableContexts);
            Thread.sleep(500L);
            userService.updateContextRolesForUser(UUID.fromString(userId), user);
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
        if (file.delete()) {
            log.info("Batch users file successfully deleted: {}", file.getAbsolutePath());
        } else {
            log.error("Failed to delete batch users file: {}", file.getAbsolutePath());
        }
    }

    List<BatchUpdateContextRolesForUserDto> fetchUsersFile(boolean removeFilesAfterFetch) {
        File directory = new File(batchUsersFileLocation);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("Batch users directory does not exist or is not a directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.warn("No .csv files found in directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        List<BatchUpdateContextRolesForUserDto> allUsers = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                List<BatchUpdateContextRolesForUserDto> users = csvParserService.transform(fis);
                allUsers.addAll(users);
                if (removeFilesAfterFetch) {
                    deleteFile(file);
                }
            } catch (IOException e) {
                log.error("Error processing batch users file: {}", file.getAbsolutePath(), e);
                throw new RuntimeException("Error processing batch users file", e);
            }
        }
        return allUsers;
    }
}
