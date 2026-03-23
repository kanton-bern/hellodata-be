package ch.bedag.dap.hellodata.portal.csv.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.data.DataDomainRoleDto;
import ch.bedag.dap.hellodata.portal.user.data.UserWithBusinessRoleDto;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BatchExportServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private MetaInfoResourceService metaInfoResourceService;
    @Mock
    private HdContextRepository contextRepository;
    @InjectMocks
    private BatchExportService batchExportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCsvHeader() {
        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of());
        when(metaInfoResourceService.findAllByModuleTypeAndKind(any(ModuleType.class), any(String.class))).thenReturn(List.of());
        when(contextRepository.findAll()).thenReturn(List.of());

        String csv = batchExportService.generateBatchExportCsv();

        assertEquals("email;businessDomainRole;context;dataDomainRole;supersetRole\n", csv);
    }

    @Test
    void testUserWithDataDomainRoles() {
        UserWithBusinessRoleDto user = createUser("alice@example.com", HdRoleName.NONE,
                List.of(new DataDomainRoleDto("Domain A", "domain_a", HdRoleName.DATA_DOMAIN_ADMIN)));

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(any(ModuleType.class), any(String.class))).thenReturn(List.of());
        when(contextRepository.findAll()).thenReturn(List.of(createContext("domain_a")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        assertEquals("alice@example.com;NONE;domain_a;DATA_DOMAIN_ADMIN;", lines[1]);
    }

    @Test
    void testUserWithoutDataDomainRolesGetsNoneForAllContexts() {
        UserWithBusinessRoleDto user = createUser("bob@example.com", HdRoleName.NONE, List.of());

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(any(ModuleType.class), any(String.class))).thenReturn(List.of());
        when(contextRepository.findAll()).thenReturn(List.of(
                createContext("domain_a"),
                createContext("domain_b")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        assertEquals(3, lines.length);
        assertEquals("bob@example.com;NONE;domain_a;NONE;", lines[1]);
        assertEquals("bob@example.com;NONE;domain_b;NONE;", lines[2]);
    }

    @Test
    void testUserWithPartialDataDomainRolesGetsNoneForMissing() {
        UserWithBusinessRoleDto user = createUser("carol@example.com", HdRoleName.BUSINESS_DOMAIN_ADMIN,
                List.of(new DataDomainRoleDto("Domain A", "domain_a", HdRoleName.DATA_DOMAIN_EDITOR)));

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(any(ModuleType.class), any(String.class))).thenReturn(List.of());
        when(contextRepository.findAll()).thenReturn(List.of(
                createContext("domain_a"),
                createContext("domain_b")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        assertEquals(3, lines.length);
        assertEquals("carol@example.com;BUSINESS_DOMAIN_ADMIN;domain_a;DATA_DOMAIN_EDITOR;", lines[1]);
        assertEquals("carol@example.com;BUSINESS_DOMAIN_ADMIN;domain_b;NONE;", lines[2]);
    }

    @Test
    void testSupersetRolesIncluded() {
        UserWithBusinessRoleDto user = createUser("dave@example.com", HdRoleName.NONE,
                List.of(new DataDomainRoleDto("Domain A", "domain_a", HdRoleName.DATA_DOMAIN_VIEWER)));

        MetaInfoResourceEntity supersetResource = createSupersetResource("domain_a",
                List.of(createSubsystemUser("dave@example.com", List.of("D_dashboard_1", "RLS_01", "Admin", "BI_VIEWER"))));

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(String.class)))
                .thenReturn(List.of(supersetResource));
        when(contextRepository.findAll()).thenReturn(List.of(createContext("domain_a")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        // Admin and BI_VIEWER should be filtered out
        assertEquals("dave@example.com;NONE;domain_a;DATA_DOMAIN_VIEWER;D_dashboard_1,RLS_01", lines[1]);
    }

    @Test
    void testSupersetRolesFilterExcludesAdminAndBiRoles() {
        assertTrue(BatchExportService.isExportableSupersetRole("D_my_dashboard"));
        assertTrue(BatchExportService.isExportableSupersetRole("RLS_01"));
        assertTrue(BatchExportService.isExportableSupersetRole("Public"));
        assertFalse(BatchExportService.isExportableSupersetRole("Admin"));
        assertFalse(BatchExportService.isExportableSupersetRole("BI_ADMIN"));
        assertFalse(BatchExportService.isExportableSupersetRole("BI_VIEWER"));
        assertFalse(BatchExportService.isExportableSupersetRole("BI_EDITOR"));
    }

    @Test
    void testMultipleUsersMultipleContexts() {
        UserWithBusinessRoleDto user1 = createUser("user1@example.com", HdRoleName.HELLODATA_ADMIN,
                List.of(
                        new DataDomainRoleDto("A", "ctx_a", HdRoleName.DATA_DOMAIN_ADMIN),
                        new DataDomainRoleDto("B", "ctx_b", HdRoleName.DATA_DOMAIN_EDITOR)));
        UserWithBusinessRoleDto user2 = createUser("user2@example.com", HdRoleName.NONE,
                List.of(new DataDomainRoleDto("A", "ctx_a", HdRoleName.DATA_DOMAIN_VIEWER)));

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user1, user2));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(any(ModuleType.class), any(String.class))).thenReturn(List.of());
        when(contextRepository.findAll()).thenReturn(List.of(
                createContext("ctx_a"),
                createContext("ctx_b")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        // header + 2 rows for user1 (has both contexts) + 2 rows for user2 (ctx_a + NONE for ctx_b)
        assertEquals(5, lines.length);
        assertEquals("user1@example.com;HELLODATA_ADMIN;ctx_a;DATA_DOMAIN_ADMIN;", lines[1]);
        assertEquals("user1@example.com;HELLODATA_ADMIN;ctx_b;DATA_DOMAIN_EDITOR;", lines[2]);
        assertEquals("user2@example.com;NONE;ctx_a;DATA_DOMAIN_VIEWER;", lines[3]);
        assertEquals("user2@example.com;NONE;ctx_b;NONE;", lines[4]);
    }

    @Test
    void testNullBusinessDomainRoleDefaultsToNone() {
        UserWithBusinessRoleDto user = createUser("null-role@example.com", null,
                List.of(new DataDomainRoleDto("A", "domain_a", HdRoleName.DATA_DOMAIN_VIEWER)));

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(any(ModuleType.class), any(String.class))).thenReturn(List.of());
        when(contextRepository.findAll()).thenReturn(List.of(createContext("domain_a")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        assertTrue(lines[1].startsWith("null-role@example.com;NONE;"));
    }

    @Test
    void testCsvIsCompatibleWithImportFormat() {
        UserWithBusinessRoleDto user = createUser("test@example.com", HdRoleName.NONE,
                List.of(new DataDomainRoleDto("Domain", "my_domain", HdRoleName.DATA_DOMAIN_VIEWER)));

        MetaInfoResourceEntity supersetResource = createSupersetResource("my_domain",
                List.of(createSubsystemUser("test@example.com", List.of("D_dash_1", "D_dash_2"))));

        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(String.class)))
                .thenReturn(List.of(supersetResource));
        when(contextRepository.findAll()).thenReturn(List.of(createContext("my_domain")));

        String csv = batchExportService.generateBatchExportCsv();
        String[] lines = csv.split("\n");

        // Verify header matches import expectation
        assertEquals("email;businessDomainRole;context;dataDomainRole;supersetRole", lines[0]);

        // Verify data row has exactly 5 semicolon-delimited fields
        String[] fields = lines[1].split(";", -1);
        assertEquals(5, fields.length);
        assertEquals("test@example.com", fields[0]);
        assertEquals("NONE", fields[1]);
        assertEquals("my_domain", fields[2]);
        assertEquals("DATA_DOMAIN_VIEWER", fields[3]);
        assertEquals("D_dash_1,D_dash_2", fields[4]);
    }

    // --- Helper methods ---

    private UserWithBusinessRoleDto createUser(String email, HdRoleName businessRole, List<DataDomainRoleDto> ddRoles) {
        UserWithBusinessRoleDto user = new UserWithBusinessRoleDto();
        user.setEmail(email);
        user.setBusinessDomainRole(businessRole);
        user.setDataDomainRoles(ddRoles);
        return user;
    }

    private HdContextEntity createContext(String contextKey) {
        HdContextEntity context = new HdContextEntity();
        context.setContextKey(contextKey);
        context.setName(contextKey);
        context.setType(HdContextType.DATA_DOMAIN);
        return context;
    }

    private SubsystemUser createSubsystemUser(String email, List<String> roleNames) {
        SubsystemUser user = new SubsystemUser();
        user.setEmail(email);
        List<SubsystemRole> roles = new ArrayList<>();
        for (int i = 0; i < roleNames.size(); i++) {
            SubsystemRole role = new SubsystemRole();
            role.setId(i);
            role.setName(roleNames.get(i));
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }

    @SuppressWarnings("unchecked")
    private MetaInfoResourceEntity createSupersetResource(String contextKey, List<SubsystemUser> users) {
        UserResource userResource = new UserResource(ModuleType.SUPERSET, "superset-instance", users);
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        entity.setContextKey(contextKey);
        entity.setInstanceName("superset-instance");
        entity.setModuleType(ModuleType.SUPERSET);
        entity.setMetainfo(userResource);
        return entity;
    }
}
