package ch.bedag.dap.hellodata.portal.csv.service;

import ch.bedag.dap.hellodata.portal.csv.data.CsvUserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static ch.bedag.dap.hellodata.portal.csv.service.CsvParserService.CSV_HEADERS;
import static ch.bedag.dap.hellodata.portal.csv.service.CsvParserService.CSV_HEADERS_WITH_DASHBOARD_GROUP;
import static org.junit.jupiter.api.Assertions.*;

//NOSONAR
class CsvParserServiceTest {
    private static final String TEST_FILE_PATH = "csv/many_users/batchprocessing_user_roles.csv";
    @InjectMocks
    private CsvParserService csvParserService;

    private static InputStream getCsvFileFromResources() {
        return CsvParserServiceTest.class.getClassLoader().getResourceAsStream(TEST_FILE_PATH);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testParseCsv() throws IOException {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                user1@example.com;NONE;context1;DATA_DOMAIN_VIEWER;roleA,roleB;GroupX,GroupY
                user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<CsvUserRole> result = csvParserService.parseCsvFile(inputStream);

        assertNotNull(result);
        assertEquals(2, result.size());

        CsvUserRole user1 = result.get(0);
        assertEquals("user1@example.com", user1.email());
        assertEquals("NONE", user1.businessDomainRole());
        assertEquals("context1", user1.context());
        assertEquals("DATA_DOMAIN_VIEWER", user1.dataDomainRole());
        assertEquals(List.of("roleA", "roleB"), user1.supersetRoles());
        assertEquals(List.of("GroupX", "GroupY"), user1.dashboardGroups());

        CsvUserRole user2 = result.get(1);
        assertEquals("user2@example.com", user2.email());
        assertEquals("NONE", user2.businessDomainRole());
        assertEquals("context2", user2.context());
        assertEquals("DATA_DOMAIN_ADMIN", user2.dataDomainRole());
        assertTrue(user2.supersetRoles().isEmpty());
        assertTrue(user2.dashboardGroups().isEmpty());
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_bad_email() { //NOSONAR
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                user1@example@.com;NONE;context1;DATA_DOMAIN_VIEWER;roleA,roleB;
                user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_bad_email_without_domain() {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                user1@example.com@;NONE;context1;DATA_DOMAIN_VIEWER;roleA,roleB;
                user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_bad_email_without_tld() {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                lore@dana;NONE;dd02;DATA_DOMAIN_EDITOR;;
                lore@dana;NONE;dd01;DATA_DOMAIN_VIEWER;;
                sanda@ccc.com;HELLODATA_ADMIN;dd01;DATA_DOMAIN_ADMIN;;
                cori@na.ch;BUSINESS_Domain_ADMIN;dd02;DATA_DOMAIN_ADMIN;;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });
        assertEquals("Email is not valid lore@dana", illegalArgumentException.getMessage());
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_bad_business_domain_role() {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                user1@example.com;NONEaaa;context1;DATA_DOMAIN_VIEWER;roleA,roleB;
                user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_bad_data_domain_role() {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                user1@example.com;NONE;context1;DATA_DOMAIN_VIEWERaaa;roleA,roleB;
                user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_missing_value() {
        String csvContent =
                "email;businessDomainRole;context;dataDomainRole;supersetRole\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });

        assertEquals("No records found in the CSV file!", illegalArgumentException.getMessage());
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_missing_header() {
        String csvContent =
                "user1@example.com;NONE;context1;DATA_DOMAIN_VIEWER;roleA,roleB\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            csvParserService.parseCsvFile(inputStream);
        });
        assertEquals("No proper headers found in the CSV file! Should have: " + Arrays.toString(CSV_HEADERS) + " or " + Arrays.toString(CSV_HEADERS_WITH_DASHBOARD_GROUP), illegalArgumentException.getMessage());
    }

    @Test
    void testParseCsvFile() throws IOException { //NOSONAR
        try (InputStream csvFileFromResources = getCsvFileFromResources()) {
            List<CsvUserRole> result = csvParserService.parseCsvFile(csvFileFromResources);
            assertNotNull(result);
            assertEquals(7, result.size());

            CsvUserRole user1 = result.get(0);
            assertEquals("john.doe@example.com", user1.email());
            assertEquals("NONE", user1.businessDomainRole());
            assertEquals("some_data_domain_key", user1.context());
            assertEquals("DATA_DOMAIN_VIEWER", user1.dataDomainRole());
            List<String> expectedRolesU1 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_01".split(","))
                    .toList();
            assertIterableEquals(expectedRolesU1, user1.supersetRoles());
            assertEquals(List.of("GroupA", "GroupB"), user1.dashboardGroups());

            CsvUserRole user2 = result.get(1);
            assertEquals("jane.smith@example.com", user2.email());
            assertEquals("NONE", user2.businessDomainRole());
            assertEquals("some_data_domain_key", user2.context());
            assertEquals("DATA_DOMAIN_VIEWER", user2.dataDomainRole());
            List<String> expectedRolesU2 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_03".split(","))
                    .toList();
            assertIterableEquals(expectedRolesU2, user2.supersetRoles());
            assertEquals(List.of("GroupA"), user2.dashboardGroups());

            CsvUserRole user3 = result.get(2);
            assertEquals("alice.johnson@example.com", user3.email());
            assertEquals("NONE", user3.businessDomainRole());
            assertEquals("some_data_domain_key", user3.context());
            assertEquals("DATA_DOMAIN_VIEWER", user3.dataDomainRole());
            List<String> expectedRolesU3 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_04".split(","))
                    .toList();
            assertIterableEquals(expectedRolesU3, user3.supersetRoles());
            assertTrue(user3.dashboardGroups().isEmpty());

            CsvUserRole user4 = result.get(3);
            assertEquals("bob.williams@example.com", user4.email());
            assertEquals("NONE", user4.businessDomainRole());
            assertEquals("some_data_domain_key", user4.context());
            assertEquals("DATA_DOMAIN_ADMIN", user4.dataDomainRole());
            List<String> expectedRolesU4 = List.of();
            assertIterableEquals(expectedRolesU4, user4.supersetRoles());
            assertTrue(user4.dashboardGroups().isEmpty());

            CsvUserRole user5 = result.get(4);
            assertEquals("charlie.brown@example.com", user5.email());
            assertEquals("NONE", user5.businessDomainRole());
            assertEquals("some_data_domain_key", user5.context());
            assertEquals("DATA_DOMAIN_VIEWER", user5.dataDomainRole());
            List<String> expectedRolesU5 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_05".split(","))
                    .toList();
            assertIterableEquals(expectedRolesU5, user5.supersetRoles());
            assertEquals(List.of("GroupB"), user5.dashboardGroups());

            CsvUserRole user6 = result.get(5);
            assertEquals("laura.anderson@example.com", user6.email());
            assertEquals("HELLODATA_ADMIN", user6.businessDomainRole());
            assertEquals("some_data_domain_key", user6.context());
            assertEquals("DATA_DOMAIN_ADMIN", user6.dataDomainRole());
            List<String> expectedRolesU6 = List.of();
            assertIterableEquals(expectedRolesU6, user6.supersetRoles());
            assertTrue(user6.dashboardGroups().isEmpty());

            CsvUserRole user7 = result.get(6);
            assertEquals("michael.taylor@example.com", user7.email());
            assertEquals("NONE", user7.businessDomainRole());
            assertEquals("some_data_domain_key", user7.context());
            assertEquals("DATA_DOMAIN_VIEWER", user7.dataDomainRole());
            List<String> expectedRolesU7 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_06".split(","))
                    .toList();
            assertIterableEquals(expectedRolesU7, user7.supersetRoles());
            assertTrue(user7.dashboardGroups().isEmpty());
        }
    }

    @Test
    void testParseCsvFile_oldFormatWithoutDashboardGroup() throws IOException {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole
                user1@example.com;NONE;context1;DATA_DOMAIN_VIEWER;roleA,roleB
                user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<CsvUserRole> result = csvParserService.parseCsvFile(inputStream);

        assertNotNull(result);
        assertEquals(2, result.size());

        CsvUserRole user1 = result.get(0);
        assertEquals("user1@example.com", user1.email());
        assertEquals(List.of("roleA", "roleB"), user1.supersetRoles());
        assertTrue(user1.dashboardGroups().isEmpty(), "Old format should have empty dashboardGroups");

        CsvUserRole user2 = result.get(1);
        assertEquals("user2@example.com", user2.email());
        assertTrue(user2.supersetRoles().isEmpty());
        assertTrue(user2.dashboardGroups().isEmpty(), "Old format should have empty dashboardGroups");
    }

    @Test
    void testParseCsvFile_dashboardGroupForAdmin_shouldBeEmptyInResult() throws IOException {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                admin@example.com;NONE;context1;DATA_DOMAIN_ADMIN;;GroupX
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<CsvUserRole> result = csvParserService.parseCsvFile(inputStream);

        assertNotNull(result);
        assertEquals(1, result.size());
        CsvUserRole admin = result.get(0);
        assertEquals("admin@example.com", admin.email());
        assertTrue(admin.supersetRoles().isEmpty());
        // dashboardGroups are parsed but only applied to VIEWER/BUSINESS_SPECIALIST - however the parsing keeps them
        assertEquals(List.of("GroupX"), admin.dashboardGroups());
    }

    @Test
    void testParseCsvFile_missingTrailingColumns_shouldNotThrowIndexOutOfBounds() throws IOException {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole
                blaba@dana.com;NONE;some_context;DATA_DOMAIN_ADMIN
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<CsvUserRole> result = csvParserService.parseCsvFile(inputStream);

        assertNotNull(result);
        assertEquals(1, result.size());

        CsvUserRole user = result.get(0);
        assertEquals("blaba@dana.com", user.email());
        assertEquals("NONE", user.businessDomainRole());
        assertEquals("some_context", user.context());
        assertEquals("DATA_DOMAIN_ADMIN", user.dataDomainRole());
        assertTrue(user.supersetRoles().isEmpty());
        assertTrue(user.dashboardGroups().isEmpty());
    }

    @Test
    void testParseCsvFile_should_throw_exception_on_empty_context() {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole
                blaba@dana.com;NONE;;DATA_DOMAIN_ADMIN;
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                csvParserService.parseCsvFile(inputStream));
        assertEquals("Context must not be empty", exception.getMessage());
    }

    @Test
    void testParseCsvFile_multipleDashboardGroups() throws IOException {
        String csvContent = """
                email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup
                user1@example.com;NONE;context1;DATA_DOMAIN_VIEWER;D_dash1;Group1,Group2,Group3
                """;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        List<CsvUserRole> result = csvParserService.parseCsvFile(inputStream);

        assertNotNull(result);
        assertEquals(1, result.size());
        CsvUserRole user = result.get(0);
        assertEquals(List.of("Group1", "Group2", "Group3"), user.dashboardGroups());
    }

}