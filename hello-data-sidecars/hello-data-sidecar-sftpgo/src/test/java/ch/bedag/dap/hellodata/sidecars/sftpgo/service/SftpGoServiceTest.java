package ch.bedag.dap.hellodata.sidecars.sftpgo.service;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.FoldersApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.GroupsApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.api.TokenApi;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.auth.HttpBasicAuth;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.auth.HttpBearerAuth;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.BaseVirtualFolder;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Group;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Token;
import ch.bedag.dap.hellodata.sidecars.sftpgo.config.S3ConnectionsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.modelmapper.ModelMapper;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SftpGoServiceTest {
    @Mock
    private ApiClient apiClient;
    @Mock
    private S3ConnectionsConfig s3ConnectionsConfig;
    @Mock
    private S3ConnectionsConfig.S3Connection s3Connection;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private GroupsApi groupsApi;
    @Mock
    private FoldersApi foldersApi;

    @InjectMocks
    private SftpGoService sftpGoService;

    private MockedConstruction<TokenApi> tokenApiMockedConstruction;
    private MockedConstruction<GroupsApi> groupsApiMockedConstruction;
    private MockedConstruction<FoldersApi> folderApiMockedConstruction;

    @BeforeEach
    void setup() {
        // mock auth objects
        HttpBasicAuth basicAuth = mock(HttpBasicAuth.class);
        HttpBearerAuth bearerAuth = mock(HttpBearerAuth.class);
        when(apiClient.getAuthentication("BasicAuth")).thenReturn(basicAuth);
        when(apiClient.getAuthentication("BearerAuth")).thenReturn(bearerAuth);

        // mock token API
        tokenApiMockedConstruction = mockConstruction(TokenApi.class, (mock, ctx) -> {
            Token token = new Token();
            token.setAccessToken("dummy-token");
            when(mock.getToken(null)).thenReturn(Mono.just(token));
        });

        S3ConnectionsConfig.SftpGo sftpGoConfig = mock(S3ConnectionsConfig.SftpGo.class);
        when(sftpGoConfig.getAdminUsername()).thenReturn("admin");
        when(sftpGoConfig.getAdminPassword()).thenReturn("secret");
        when(s3ConnectionsConfig.getSftpGo()).thenReturn(sftpGoConfig);
    }

    @AfterEach
    void tearDown() {
        tokenApiMockedConstruction.close();
        if (groupsApiMockedConstruction != null) {
            groupsApiMockedConstruction.close();
        }
        if (folderApiMockedConstruction != null) {
            folderApiMockedConstruction.close();
        }
    }

    @Test
    void shouldInitializeWithoutCreatingAdminGroupWhenItExists() {
        // given
        Group existingGroup = new Group();
        existingGroup.setName("Admin");

        // mock GroupsApi
        groupsApiMockedConstruction = mockGroupsApi(existingGroup, false);

        // when / then
        assertThatNoException().isThrownBy(() -> sftpGoService.init());

        verify(groupsApi, never()).addGroup(any(), anyInt());
        verify(groupsApi, times(1)).getGroupByName(eq(SftpGoService.ADMIN_GROUP_NAME), anyInt());
    }

    @Test
    void shouldCreateAdminGroupWhenNotFound() {
        // given
        groupsApiMockedConstruction = mockGroupsApi(null, true);

        BaseVirtualFolder createdFolder = new BaseVirtualFolder();
        createdFolder.setId(123);
        createdFolder.setName("Admin");
        createdFolder.setMappedPath("/admin");

        // mock folder creation
        folderApiMockedConstruction = mockFoldersApi(createdFolder);

        // when
        sftpGoService.init();

        // then
        verify(groupsApi, times(1)).addGroup(any(Group.class), eq(0));
        verify(foldersApi, times(1)).addFolder(any(BaseVirtualFolder.class), eq(0));
    }

    // --- helper mocks ---

    private MockedConstruction<GroupsApi> mockGroupsApi(Group existingGroup, boolean throwNotFound) {
        return mockConstruction(GroupsApi.class, (mock, ctx) -> {
            when(mock.getGroupByName(eq(SftpGoService.ADMIN_GROUP_NAME), anyInt()))
                    .thenReturn(
                            throwNotFound
                                    ? Mono.error(WebClientResponseException.NotFound.create(404, "not found", null, null, null))
                                    : Mono.just(existingGroup)
                    );
            doReturn(Mono.empty()).when(mock).addGroup(any(Group.class), anyInt());
            groupsApi = mock;
        });
    }

    private MockedConstruction<FoldersApi> mockFoldersApi(BaseVirtualFolder createdFolder) {
        return mockConstruction(FoldersApi.class, (mock, ctx) -> {
            when(mock.addFolder(any(BaseVirtualFolder.class), anyInt()))
                    .thenReturn(Mono.just(createdFolder));
            foldersApi = mock;
        });
    }
}