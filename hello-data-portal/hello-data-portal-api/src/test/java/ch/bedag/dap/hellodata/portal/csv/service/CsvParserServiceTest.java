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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserServiceTest {
    private static final String TEST_FILE_PATH = "csv/batchprocessing_user_roles.csv";
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
    void testParseCsv() {
        String csvContent = "email;businessDomainRole;context;dataDomainRole;supersetRole\n" +
                "user1@example.com;NONE;context1;DATA_DOMAIN_VIEWER;roleA,roleB\n" +
                "user2@example.com;NONE;context2;DATA_DOMAIN_ADMIN;\n";
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

        CsvUserRole user2 = result.get(1);
        assertEquals("user2@example.com", user2.email());
        assertEquals("NONE", user2.businessDomainRole());
        assertEquals("context2", user2.context());
        assertEquals("DATA_DOMAIN_ADMIN", user2.dataDomainRole());
        assertTrue(user2.supersetRoles().isEmpty());
    }

    @Test
    void testParseCsvFile() throws IOException {
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
                    .collect(Collectors.toList());
            assertIterableEquals(expectedRolesU1, user1.supersetRoles());

            CsvUserRole user2 = result.get(1);
            assertEquals("jane.smith@example.com", user2.email());
            assertEquals("NONE", user2.businessDomainRole());
            assertEquals("some_data_domain_key", user2.context());
            assertEquals("DATA_DOMAIN_VIEWER", user2.dataDomainRole());
            List<String> expectedRolesU2 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_03".split(","))
                    .collect(Collectors.toList());
            assertIterableEquals(expectedRolesU2, user2.supersetRoles());

            CsvUserRole user3 = result.get(2);
            assertEquals("alice.johnson@example.com", user3.email());
            assertEquals("NONE", user3.businessDomainRole());
            assertEquals("some_data_domain_key", user3.context());
            assertEquals("DATA_DOMAIN_VIEWER", user3.dataDomainRole());
            List<String> expectedRolesU3 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_04".split(","))
                    .collect(Collectors.toList());
            assertIterableEquals(expectedRolesU3, user3.supersetRoles());

            CsvUserRole user4 = result.get(3);
            assertEquals("bob.williams@example.com", user4.email());
            assertEquals("NONE", user4.businessDomainRole());
            assertEquals("some_data_domain_key", user4.context());
            assertEquals("DATA_DOMAIN_ADMIN", user4.dataDomainRole());
            List<String> expectedRolesU4 = List.of();
            assertIterableEquals(expectedRolesU4, user4.supersetRoles());

            CsvUserRole user5 = result.get(4);
            assertEquals("charlie.brown@example.com", user5.email());
            assertEquals("NONE", user5.businessDomainRole());
            assertEquals("some_data_domain_key", user5.context());
            assertEquals("DATA_DOMAIN_VIEWER", user5.dataDomainRole());
            List<String> expectedRolesU5 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_05".split(","))
                    .collect(Collectors.toList());
            assertIterableEquals(expectedRolesU5, user5.supersetRoles());

            CsvUserRole user6 = result.get(5);
            assertEquals("laura.anderson@example.com", user6.email());
            assertEquals("HELLODATA_ADMIN", user6.businessDomainRole());
            assertEquals("some_data_domain_key", user6.context());
            assertEquals("DATA_DOMAIN_ADMIN", user6.dataDomainRole());
            List<String> expectedRolesU6 = List.of();
            assertIterableEquals(expectedRolesU6, user6.supersetRoles());

            CsvUserRole user7 = result.get(6);
            assertEquals("michael.taylor@example.com", user7.email());
            assertEquals("NONE", user7.businessDomainRole());
            assertEquals("some_data_domain_key", user7.context());
            assertEquals("DATA_DOMAIN_VIEWER", user7.dataDomainRole());
            List<String> expectedRolesU7 = Arrays.stream("D_test_dashboard_6,D_example_dashboard_2,RLS_06".split(","))
                    .collect(Collectors.toList());
            assertIterableEquals(expectedRolesU7, user7.supersetRoles());
        }
    }

}