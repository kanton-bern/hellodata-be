package ch.bedag.dap.hellodata.portal.excel.service;

import ch.bedag.dap.hellodata.portal.excel.data.BatchUpdateContextRolesForUserDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelParserServiceTest {
    private static final String TEST_FILE_PATH = "users.xlsx";

    private final ExcelParserService excelParserService = new ExcelParserService();

    private static InputStream getExcelFileFromResources() {
        return ExcelParserServiceTest.class.getClassLoader().getResourceAsStream(TEST_FILE_PATH);
    }

    @Test
    void testParseExcel() throws IOException {
        try (InputStream excelStream = getExcelFileFromResources()) {
            assertNotNull(excelStream, "Excel file not found in classpath!");

            List<Map<String, String>> parsedData = excelParserService.parseExcel(excelStream);

            assertEquals(8, parsedData.size());

            //first row
            int row = 0;
            assertEquals("some.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("showcase", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("6.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Showcase dashboard 1", parsedData.get(row).get("dashboardTitle"));

            //second row
            row++;
            assertEquals("some.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("showcase", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("1.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Showcase dashboard 2", parsedData.get(row).get("dashboardTitle"));

            //third row
            row++;
            assertEquals("some.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("demo", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("2.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Demo dashboard 1", parsedData.get(row).get("dashboardTitle"));

            //fourth row
            row++;
            assertEquals("some.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("demo", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("5.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Demo dashboard 2", parsedData.get(row).get("dashboardTitle"));

            //fifth row
            row++;
            assertEquals("some.other.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("showcase", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("6.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Showcase dashboard 1", parsedData.get(row).get("dashboardTitle"));

            //sixth row
            row++;
            assertEquals("some.other.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("showcase", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("1.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Showcase dashboard 2", parsedData.get(row).get("dashboardTitle"));

            //seventh row
            row++;
            assertEquals("some.other.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("demo", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("2.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Demo dashboard 1", parsedData.get(row).get("dashboardTitle"));

            //eighth row
            row++;
            assertEquals("some.other.email@example.com", parsedData.get(row).get("email"));
            assertEquals("NONE", parsedData.get(row).get("businessDomainRole"));
            assertEquals("demo", parsedData.get(row).get("context"));
            assertEquals("DATA_DOMAIN_VIEWER", parsedData.get(row).get("dataDomainRole"));
            assertEquals("5.0", parsedData.get(row).get("dashboardId"));
            assertEquals("Demo dashboard 2", parsedData.get(row).get("dashboardTitle"));
        }
    }

    @Test
    void testTransform() throws IOException {
        try (InputStream excelStream = getExcelFileFromResources()) {
            List<BatchUpdateContextRolesForUserDto> transform = excelParserService.transform(excelStream);
            assertEquals(transform.size(), 2);

            //First user
            BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto = transform.get(0);
            assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "some.email@example.com");

            assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "NONE");

            assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().size(), 2);
            assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_VIEWER");
            assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey(), "showcase");
            assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getRole().getName(), "DATA_DOMAIN_VIEWER");
            assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getContext().getContextKey(), "demo");

            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(0).getTitle(), "Showcase dashboard 1");
            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(1).getTitle(), "Showcase dashboard 2");
            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(0).getId(), 6);
            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(1).getId(), 1);

            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(0).getTitle(), "Demo dashboard 1");
            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(1).getTitle(), "Demo dashboard 2");
            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(0).getId(), 2);
            assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(1).getId(), 5);


            //Second user
            BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto1 = transform.get(1);
            assertEquals(batchUpdateContextRolesForUserDto1.getEmail(), "some.other.email@example.com");

            assertEquals(batchUpdateContextRolesForUserDto1.getBusinessDomainRole().getName(), "NONE");

            assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().size(), 2);
            assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_VIEWER");
            assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(0).getContext().getContextKey(), "showcase");
            assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(1).getRole().getName(), "DATA_DOMAIN_VIEWER");
            assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(1).getContext().getContextKey(), "demo");

            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(0).getTitle(), "Showcase dashboard 1");
            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(1).getTitle(), "Showcase dashboard 2");
            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(0).getId(), 6);
            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(1).getId(), 1);

            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(0).getTitle(), "Demo dashboard 1");
            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(1).getTitle(), "Demo dashboard 2");
            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(0).getId(), 2);
            assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(1).getId(), 5);
        }

    }
}