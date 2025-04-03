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
package ch.bedag.dap.hellodata.portal.user.controller;

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.portal.base.config.SystemProperties;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final HelloDataContextConfig helloDataContextConfig;
    private final SystemProperties systemProperties;


    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public CreateUserResponseDto createUser(@RequestBody @Valid @NotNull CreateUserRequestDto createUserRequestDto) {
        try {
            String email = createUserRequestDto.getUser().getEmail();
            if (!EmailValidator.getInstance().isValid(email)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
            }
            String firstName = createUserRequestDto.getUser().getFirstName();
            String lastName = createUserRequestDto.getUser().getLastName();
            AdUserOrigin origin = createUserRequestDto.getUser().getOrigin();
            String userId = userService.createUser(email, firstName, lastName, origin);
            return CreateUserResponseDto.builder().userId(userId).build();
        } catch (ClientErrorException e) {
            log.error("Error on user creation", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        } catch (Exception e) {
            log.error("", e);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {

        sort = StringUtils.defaultIfEmpty(sort, null);
        search = StringUtils.defaultIfEmpty(search, null);

        Sort sorting = Sort.by(Sort.Direction.ASC, "id");
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String sortField = sortParams[0];
                Sort.Direction direction = Sort.Direction.fromString(sortParams[1].trim());
                sorting = Sort.by(direction, sortField);
            }
        }
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<UserDto> usersPage = userService.getAllUsersPageable(pageable, search);
        return ResponseEntity.ok(usersPage);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public UserDto getUserById(@PathVariable String userId) {
        try {
            return userService.getUserById(userId);
        } catch (ClientErrorException e) {
            log.error("Error on getting user by id", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        }
    }

    @GetMapping("/admin-emails")
    public List<String> getAdminEmails() {
        List<UserEntity> users = userService.findHelloDataAdminUsers();
        return users.stream().map(UserEntity::getEmail).collect(Collectors.toList());
    }

    @GetMapping("/current/profile")
    public CurrentUserDto getPermissionsForCurrentUser() {
        try {
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                userService.updateLastAccess(currentUserId.toString());
                return new CurrentUserDto(SecurityUtils.getCurrentUserEmail(), getCurrentUserPermissions(), SecurityUtils.isSuperuser(),
                        helloDataContextConfig.getBusinessContext().getName(), systemProperties.isDisableLogout(),
                        userService.isUserDisabled(currentUserId.toString()), userService.getSelectedLanguage(currentUserId.toString())
                );
            }
            return new CurrentUserDto(SecurityUtils.getCurrentUserEmail(), getCurrentUserPermissions(), SecurityUtils.isSuperuser(),
                    helloDataContextConfig.getBusinessContext().getName(), systemProperties.isDisableLogout(),
                    false, Locale.ROOT);
        } catch (ClientErrorException e) {
            log.error("Error on getting user sessions", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        }
    }

    @GetMapping("/current/context-roles")
    public List<UserContextRoleDto> getContextRolesForCurrentUser() {
        try {
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                return userService.getContextRolesForUser(currentUserId);
            }
            return new ArrayList<>();
        } catch (ClientErrorException e) {
            log.error("Error on getting user sessions", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public void deleteUserById(@PathVariable String userId) {
        try {
            userService.deleteUserById(userId);
        } catch (ClientErrorException e) {
            log.error("Error on user deletion", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        }
    }

    @PatchMapping("/{userId}/disable")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public UserDto disableUserById(@PathVariable String userId) {
        try {
            return userService.disableUserById(userId);
        } catch (ClientErrorException e) {
            log.error("Error on user disable", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        }
    }

    @PatchMapping("/{userId}/enable")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public UserDto enableUserById(@PathVariable String userId) {
        try {
            return userService.enableUserById(userId);
        } catch (ClientErrorException e) {
            log.error("Error on user enable", e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(e.getResponse().getStatus()));
        }
    }

    @GetMapping("/{userId}/dashboards")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public DashboardsDto getDashboardsMarkUser(@PathVariable String userId) {
        return userService.getDashboardsMarkUser(userId);
    }

    @GetMapping("/contexts")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public ContextsDto getAvailableContexts() {
        return userService.getAvailableContexts();
    }

    @GetMapping("/{userId}/context-roles")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public List<UserContextRoleDto> getContextRolesForUser(@PathVariable UUID userId) {
        return userService.getContextRolesForUser(userId);
    }

    @PatchMapping("/{userId}/context-roles")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public void updateContextRolesForUser(@PathVariable UUID userId, @NotNull @Valid @RequestBody UpdateContextRolesForUserDto updateContextRolesForUserDto) {
        userService.updateContextRolesForUser(userId, updateContextRolesForUserDto);
    }

    @GetMapping("search/{email}")
    @PreAuthorize("hasAnyAuthority('USER_MANAGEMENT')")
    public List<AdUserDto> searchUser(@PathVariable String email) {
        return userService.searchUser(email);
    }

    /**
     * Returns all available data domains for current user
     *
     * @return list of data domains
     */
    @GetMapping("/data-domains")
    public List<DataDomainDto> getAvailableDataDomains() {
        return userService.getAvailableDataDomains();
    }

    @PatchMapping("/{userId}/set-selected-lang/{lang}")
    public void setSelectedLanguageForUser(@PathVariable String userId, @PathVariable Locale lang) {
        userService.setSelectedLanguage(userId, lang);
    }

    private Set<String> getCurrentUserPermissions() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            return userService.getUserPortalPermissions(currentUserId);
        }
        return new HashSet<>();
    }
}
