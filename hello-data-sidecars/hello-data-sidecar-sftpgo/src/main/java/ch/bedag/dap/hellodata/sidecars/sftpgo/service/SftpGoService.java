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
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

    private OffsetDateTime lastTokenRefreshTime;

    @PostConstruct
    public void init() {
        log.info("Initializing SFTP Go API, username {}", s3ConnectionsConfig.getSftpGo().getAdminUsername());
        log.info("Initializing SFTP Go API, pass [first 3 letters] {}", s3ConnectionsConfig.getSftpGo().getAdminPassword().substring(0, 3));
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

    public void deleteUser(String username) {
        refreshToken();
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        usersApi.deleteUser(username).block();
        log.info("User {} deleted", username);
    }


    public void updateUser(User user) {
        refreshToken();
        UsersApi usersApi = new UsersApi(sftpGoApiClient);
        usersApi.updateUser(user.getEmail(), user, 1).block();
        log.info("User {} updated", user.getEmail());
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
        return createdUser;
    }

    /**
     * Create or update a group with the given name and permissions
     *
     * @param dataDomainKey  - key of the data domain
     * @param dataDomainName - name of the data domain
     * @param groupName      - name of the group to create or update
     * @param permissions    - list of permissions to set for the group
     * @param updateGroup    - if true, update the group if it already exists
     */
    public void createOrUpdateGroup(String dataDomainKey, String dataDomainName, String groupName, List<Permission> permissions, boolean updateGroup) {
        refreshToken();
        GroupsApi groupsApi = new GroupsApi(sftpGoApiClient);
        try {
            Group existingGroup = groupsApi.getGroupByName(groupName, 0).block();
            log.info("Group {} already exists", existingGroup.getName());
            if (updateGroup) {
                log.info("Configuration changed, updating group {}", groupName);
                existingGroup.getVirtualFolders().forEach(virtualFolder -> {
                    updateVirtualFolder(virtualFolder, dataDomainKey, dataDomainName);
                });
            }
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
        baseVirtualFolder.setMappedPath(s3ConnectionsConfig.getAdminVirtualFolder());
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
        virtualFolder.setVirtualPath(s3ConnectionsConfig.getAdminVirtualFolder());
        virtualFolder.setId(createdFolder.getId());
        List<VirtualFolder> virtualFolders = new ArrayList<>();
        virtualFolders.add(virtualFolder);
        group.setVirtualFolders(virtualFolders);
        return group;
    }

    private VirtualFolder createVirtualFolder(String dataDomainKey, String dataDomainName, String groupName) {
        FilesystemConfig filesystemConfig = generateFilesystemConfig(dataDomainKey);

        BaseVirtualFolder baseVirtualFolder = new BaseVirtualFolder();
        baseVirtualFolder.setName(groupName);
        baseVirtualFolder.setDescription(dataDomainName);
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

        return getVirtualFolder(createdFolder, dataDomainName);
    }

    VirtualFolder updateVirtualFolder(VirtualFolder virtualFolder, String dataDomainKey, String dataDomainName) {
        FilesystemConfig filesystemConfig = generateFilesystemConfig(dataDomainKey);
        virtualFolder.setFilesystem(filesystemConfig);
        FoldersApi foldersApi = new FoldersApi(sftpGoApiClient);
        log.info("Updating folder {}", virtualFolder);
        BaseVirtualFolder baseVirtualFolder = modelMapper.map(virtualFolder, BaseVirtualFolder.class);

        ModelApiResponse response = foldersApi.updateFolder(virtualFolder.getName(), baseVirtualFolder).block();
        log.info("Folder [{}] updated with result: {}", virtualFolder.getName(), response);

        return getVirtualFolder(baseVirtualFolder, dataDomainName);
    }

    private FilesystemConfig generateFilesystemConfig(String dataDomainKey) {
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
        return filesystemConfig;
    }

    private VirtualFolder getVirtualFolder(BaseVirtualFolder virtualFolder, String dataDomainName) {
        VirtualFolder vf = new VirtualFolder();
        vf.setName(virtualFolder.getName());
        vf.setDescription(virtualFolder.getDescription());
        vf.setMappedPath(virtualFolder.getMappedPath());
        vf.setVirtualPath("/" + dataDomainName);
        vf.setId(virtualFolder.getId());
        return vf;
    }

    /**
     * If the lastRefreshTime is set, check if 20 minutes have passed
     */
    private void refreshToken() {
        if (lastTokenRefreshTime != null) {
            Duration timeSinceLastRefresh = Duration.between(lastTokenRefreshTime, OffsetDateTime.now());
            if (timeSinceLastRefresh.toMinutes() < 19) {
                log.info("Token refresh skipped. Last refresh was {} minutes ago.", timeSinceLastRefresh.toMinutes());
                return;
            }
        }

        HttpBasicAuth basicAuth = (HttpBasicAuth) sftpGoApiClient.getAuthentication("BasicAuth");
        basicAuth.setUsername(s3ConnectionsConfig.getSftpGo().getAdminUsername());
        basicAuth.setPassword(s3ConnectionsConfig.getSftpGo().getAdminPassword());

        TokenApi tokenApi = new TokenApi(sftpGoApiClient);
        Token token = tokenApi.getToken(null).block();

        HttpBearerAuth BearerAuth = (HttpBearerAuth) sftpGoApiClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken(token.getAccessToken());

        lastTokenRefreshTime = OffsetDateTime.now();
        log.info("Token refreshed successfully. Next refresh allowed after 20 minutes.");
    }

}