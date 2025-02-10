package ch.bedag.dap.hellodata.portal.excel.service;

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.excel.data.BatchUpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ExcelParserService {

    public List<BatchUpdateContextRolesForUserDto> transform(InputStream excelStream) {
        List<Map<String, String>> parsedData = parseExcel(excelStream);
        Map<String, BatchUpdateContextRolesForUserDto> usersMap = new LinkedHashMap<>();

        for (Map<String, String> row : parsedData) {
            String email = row.get("email");
            String businessDomainRole = row.get("businessDomainRole");
            String dataDomain = row.get("context");
            String dataDomainRole = row.get("dataDomainRole");
            String dashboardIdStr = row.get("dashboardId");
            String dashboardTitle = row.get("dashboardTitle");

            usersMap.putIfAbsent(email, new BatchUpdateContextRolesForUserDto());
            BatchUpdateContextRolesForUserDto userDto = usersMap.get(email);
            userDto.setEmail(email);
            userDto.setBusinessDomainRole(new RoleDto());
            userDto.getBusinessDomainRole().setContextType(HdContextType.BUSINESS_DOMAIN);
            userDto.getBusinessDomainRole().setName(businessDomainRole);
            if (userDto.getDataDomainRoles() == null) {
                userDto.setDataDomainRoles(new ArrayList<>());
            }
            Optional<UserContextRoleDto> alreadyAdded = userDto.getDataDomainRoles().stream().filter(userContextRoleDto ->
                    userContextRoleDto.getContext().getContextKey().equals(dataDomain) && userContextRoleDto.getRole().getName().equals(dataDomainRole)).findFirst();
            if (dataDomain != null && !dataDomain.isEmpty() && dataDomainRole != null && !dataDomainRole.isEmpty() && alreadyAdded.isEmpty()) {
                UserContextRoleDto userContextRole = new UserContextRoleDto();
                userContextRole.setRole(new RoleDto());
                userContextRole.getRole().setName(dataDomainRole);
                userContextRole.getRole().setContextType(HdContextType.DATA_DOMAIN);
                ContextDto contextDto = new ContextDto();
                contextDto.setContextKey(dataDomain);
                userContextRole.setContext(contextDto);
                userDto.getDataDomainRoles().add(userContextRole);
            }

            if (dashboardIdStr != null && !dashboardIdStr.isEmpty()) {
                int dashboardId = (int) Double.parseDouble(dashboardIdStr);
                DashboardForUserDto dashboard = new DashboardForUserDto();
                dashboard.setId(dashboardId);
                dashboard.setTitle(dashboardTitle);
                dashboard.setContextKey(dataDomain);

                if (userDto.getSelectedDashboardsForUser() == null) {
                    userDto.setSelectedDashboardsForUser(new HashMap<>());
                }
                userDto.getSelectedDashboardsForUser()
                        .computeIfAbsent(dataDomain, k -> new ArrayList<>())
                        .add(dashboard);
            }
        }
        return new ArrayList<>(usersMap.values());
    }

    List<Map<String, String>> parseExcel(InputStream excelStream) {
        List<Map<String, String>> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(excelStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            List<String> headers = new ArrayList<>();
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                headerRow.forEach(cell -> headers.add(cell.getStringCellValue()));
            }

            String[] lastKnownValues = new String[headers.size()];
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowData = new LinkedHashMap<>();

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String cellValue = cell.toString().trim();

                    if (!cellValue.isEmpty()) {
                        lastKnownValues[i] = cellValue;
                    }
                    rowData.put(headers.get(i), lastKnownValues[i]);
                }
                records.add(rowData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading Excel file", e);
        }
        return records;
    }
}