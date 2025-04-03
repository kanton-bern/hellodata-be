package ch.bedag.dap.hellodata.sidecars.sftpgo.service;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.FoldersApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.GroupsApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.TokenApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.UsersApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.auth.HttpBasicAuth;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.auth.HttpBearerAuth;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.*;
import ch.bedag.dap.hellodata.sidecars.sftpgo.config.S3ConnectionsConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Permission.STAR;
import static org.springframework.web.reactive.function.client.WebClientResponseException.Conflict;
import static org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;

@Log4j2
@Service
@RequiredArgsConstructor
public class SftpGoService {
    public static final String ADMIN_GROUP_NAME = "Admin";
    private final ApiClient sftpGoApiClient;
    private final S3ConnectionsConfig s3ConnectionsConfig;

    @Value("${hello-data.sftpgo.admin-username}")
    private String sftpGoAdminUsername;
    @Value("${hello-data.sftpgo.admin-password}")
    private String sftpGoAdminPassword;
    @Value("${hello-data.admin-virtual-folder}")
    private String adminVirtualFolder;

    private OffsetDateTime lastRefreshTime;

    @PostConstruct
    public void init() {
        log.info("Initializing SFTP Go API, username {}", sftpGoAdminUsername);
        log.info("Initializing SFTP Go API, pass [first 3 letters] {}", sftpGoAdminPassword.substring(0, 3));
        initAdminGroup();
    }

    public List<User> getAllUsers() {
        refreshToken();
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        return usersApi.getUsers(0, Integer.MAX_VALUE, "ASC").collectList().block();
    }

    public User getUser(String username) {
        refreshToken();
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        return usersApi.getUserByUsername(username, 0).block();
    }

    public void disableUser(String username) {
        refreshToken();
        User user = getUser(username);
        user.setStatus(User.StatusEnum.NUMBER_0);
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        usersApi.updateUser(username, user, 1).block();
        log.info("User {} disabled", username);
    }

    public void updateUser(User user) {
        refreshToken();
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        usersApi.updateUser(user.getUsername(), user, 1).block();
        log.info("User {} updated", user.getUsername());
    }

    public void enableUser(String username) {
        refreshToken();
        User user = getUser(username);
        user.setStatus(User.StatusEnum.NUMBER_1);
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        usersApi.updateUser(username, user, 1).block();
        log.info("User {} enabled", username);
    }

    public User createUser(String email, String username, String password) {
        refreshToken();
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setStatus(User.StatusEnum.NUMBER_1);

        HashMap<String, List<Permission>> permissions = new HashMap<>();
        permissions.put("/", List.of(STAR));
        user.permissions(permissions);

        UserFilters filters = new UserFilters();
        filters.setWebClient(List.of(WebClientOptions.MFA_DISABLED));
        user.setFilters(filters);

        User createdUser = usersApi.addUser(user, 0).block();

        log.info("User {} {} created", email, createdUser != null ? "was" : "was not");
//            usersApi.disableUser2fa(createdUser.getUsername()).block(); // FIXME getting 400 error
        return createdUser;
    }

    public void createGroup(String dataDomainKey, String dataDomainName, String groupName, List<Permission> permissions) {
        refreshToken();
        GroupsApi groupsApi = new GroupsApi(sftpGoApiClient);
        try {
            Group existingGroup = groupsApi.getGroupByName(groupName, 0).block();
            log.info("Group {} already exists", existingGroup.getName());
        } catch (NotFound notFound) {
            log.debug("", notFound);
            log.info("Group {} not found, creating...", groupName);
            Group group = new Group();
            group.setName(groupName);
            group.setDescription(dataDomainName);

            VirtualFolder vf = createVirtualFolder(dataDomainKey, dataDomainName, groupName);
            group.setVirtualFolders(List.of(vf));

            GroupUserSettings groupUserSettings = new GroupUserSettings();
            Map<String, List<Permission>> permissionsMap = new HashMap<>();
            permissionsMap.put(vf.getVirtualPath() + "/", permissions);
            groupUserSettings.setPermissions(permissionsMap);
            group.setUserSettings(groupUserSettings);

            groupsApi.addGroup(group, 0).block();
            log.info("Group {} created", groupName);
        }
    }

    private void initAdminGroup() {
        refreshToken();
        GroupsApi groupsApi = new GroupsApi(sftpGoApiClient);
        try {
            Group existingGroup = groupsApi.getGroupByName(ADMIN_GROUP_NAME, 0).block();
            log.info("Admin group '{}' already exists", existingGroup);
        } catch (NotFound notFound) {
            log.debug("", notFound);
            log.info("Admin group not found, creating...");
            createAdminGroup(groupsApi);
        }
    }

    private void createAdminGroup(GroupsApi groupsApi) {
        refreshToken();
        BaseVirtualFolder baseVirtualFolder = new BaseVirtualFolder();
        baseVirtualFolder.setName(ADMIN_GROUP_NAME);
        baseVirtualFolder.setMappedPath(adminVirtualFolder);
        FoldersApi foldersApi = new FoldersApi(sftpGoApiClient);
        BaseVirtualFolder createdFolder = foldersApi.addFolder(baseVirtualFolder, 0).block();

        Group group = getGroup(createdFolder);
        groupsApi.addGroup(group, 0).block();
        log.info("Admin group created");
    }

    private Group getGroup(BaseVirtualFolder createdFolder) {
        Group group = new Group();
        group.setName(ADMIN_GROUP_NAME);
        group.setDescription("Admin group");
        VirtualFolder virtualFolder = new VirtualFolder();
        virtualFolder.setName(createdFolder.getName());
        virtualFolder.setMappedPath(createdFolder.getMappedPath());
        virtualFolder.setVirtualPath(adminVirtualFolder);
        virtualFolder.setId(createdFolder.getId());
        List<VirtualFolder> virtualFolders = new ArrayList<>();
        virtualFolders.add(virtualFolder);
        group.setVirtualFolders(virtualFolders);
        return group;
    }

    private VirtualFolder createVirtualFolder(String dataDomainKey, String dataDomainName, String groupName) {
        refreshToken();
        S3ConnectionsConfig.S3Connection s3Connection = s3ConnectionsConfig.getS3Connection(dataDomainKey);
        S3Config s3Config = new S3Config();
        s3Config.setAccessKey(s3Connection.getAccessKey());
        Secret accessSecret = new Secret();
        accessSecret.setKey(s3Connection.getAccessKey());
        accessSecret.setStatus(Secret.StatusEnum.PLAIN);
        accessSecret.setPayload(s3Connection.getAccessSecret());
        s3Config.setAccessSecret(accessSecret);
        s3Config.setEndpoint(s3Connection.getEndpoint());
        s3Config.forcePathStyle(s3Connection.isForcePathStyle());
        s3Config.setBucket(s3Connection.getBucket());
        s3Config.setRegion(s3Connection.getRegion());
        FilesystemConfig filesystemConfig = new FilesystemConfig();
        filesystemConfig.s3config(s3Config);
        filesystemConfig.setProvider(FsProviders.NUMBER_1);

        BaseVirtualFolder baseVirtualFolder = new BaseVirtualFolder();
        baseVirtualFolder.setName(groupName);
        baseVirtualFolder.setDescription(dataDomainName);
//        baseVirtualFolder.setMappedPath("/" + groupName); remove mapped path
        baseVirtualFolder.setFilesystem(filesystemConfig);
        FoldersApi foldersApi = new FoldersApi(sftpGoApiClient);
        log.info("Creating folder {}", baseVirtualFolder);

        BaseVirtualFolder createdFolder;
        try {
            createdFolder = foldersApi.addFolder(baseVirtualFolder, 0).block();
        } catch (Conflict conflict) {
            log.info("Folder {} already exists, fetching", baseVirtualFolder);
            createdFolder = foldersApi.getFolderByName(groupName, 0).block();
        }

        VirtualFolder vf = new VirtualFolder();
        vf.setName(createdFolder.getName());
        vf.setDescription(createdFolder.getDescription());
        vf.setMappedPath(createdFolder.getMappedPath());
        vf.setVirtualPath("/" + dataDomainName);
        vf.setId(createdFolder.getId());
        return vf;
    }

    /**
     * If the lastRefreshTime is set, check if 20 minutes have passed
     */
    private void refreshToken() {
        if (lastRefreshTime != null) {
            Duration timeSinceLastRefresh = Duration.between(lastRefreshTime, OffsetDateTime.now());
            if (timeSinceLastRefresh.toMinutes() < 25) {
                log.info("Token refresh skipped. Last refresh was {} minutes ago.", timeSinceLastRefresh.toMinutes());
                return;
            }
        }

        HttpBasicAuth basicAuth = (HttpBasicAuth) sftpGoApiClient.getAuthentication("BasicAuth");
        basicAuth.setUsername(sftpGoAdminUsername);
        basicAuth.setPassword(sftpGoAdminPassword);

        TokenApi tokenApi = new TokenApi(sftpGoApiClient);
        Token token = tokenApi.getToken(null).block();

        HttpBearerAuth BearerAuth = (HttpBearerAuth) sftpGoApiClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken(token.getAccessToken());

        lastRefreshTime = OffsetDateTime.now();
        log.info("Token refreshed successfully. Next refresh allowed after 20 minutes.");
    }

}