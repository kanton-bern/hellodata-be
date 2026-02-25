package ch.bedag.dap.hellodata.portal.csv.service;

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames;
import ch.bedag.dap.hellodata.portal.csv.data.CsvUserRole;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.user.data.BatchUpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class CsvParserService {

    private static final String EMAIL = "email";
    private static final String BUSINESS_DOMAIN_ROLE = "businessDomainRole";
    private static final String CONTEXT = "context";
    private static final String DATA_DOMAIN_ROLE = "dataDomainRole";
    private static final String SUPERSET_ROLE = "supersetRole";
    private static final String DASHBOARD_GROUP = "dashboardGroup";
    static final String[] CSV_HEADERS = {EMAIL, BUSINESS_DOMAIN_ROLE, CONTEXT, DATA_DOMAIN_ROLE, SUPERSET_ROLE};
    static final String[] CSV_HEADERS_WITH_DASHBOARD_GROUP = {EMAIL, BUSINESS_DOMAIN_ROLE, CONTEXT, DATA_DOMAIN_ROLE, SUPERSET_ROLE, DASHBOARD_GROUP};
    private static final char CSV_DELIMITER = ';';
    private static final String ROLE_DELIMITER = ",";

    @SneakyThrows
    public List<BatchUpdateContextRolesForUserDto> transform(InputStream csvStream) {
        List<CsvUserRole> parsedData = parseCsvFile(csvStream);
        Map<String, BatchUpdateContextRolesForUserDto> usersMap = new LinkedHashMap<>();

        for (CsvUserRole row : parsedData) {
            String email = row.email();
            String businessDomainRole = row.businessDomainRole();
            String dataDomain = row.context();
            String dataDomainRole = row.dataDomainRole();
            List<String> supersetRoles = row.supersetRoles();
            List<String> dashboardGroups = row.dashboardGroups();

            usersMap.putIfAbsent(email, new BatchUpdateContextRolesForUserDto());
            BatchUpdateContextRolesForUserDto userDto = usersMap.get(email);

            Map<String, List<ModuleRoleNames>> contextToModuleRoleNamesMap = userDto.getContextToModuleRoleNamesMap();
            contextToModuleRoleNamesMap.computeIfAbsent(dataDomain, k -> new ArrayList<>()).add(new ModuleRoleNames(ModuleType.SUPERSET, supersetRoles));

            // Map dashboard group names per context
            if (dashboardGroups != null && !dashboardGroups.isEmpty()) {
                Map<String, List<String>> dashboardGroupNamesMap = userDto.getDashboardGroupNamesFromCsv();
                dashboardGroupNamesMap.computeIfAbsent(dataDomain, k -> new ArrayList<>()).addAll(dashboardGroups);
            }

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

        }
        return new ArrayList<>(usersMap.values());
    }

    private void verifyEmail(String email) {
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            throw new IllegalArgumentException("Email is not valid %s".formatted(email));
        }
    }

    private boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

    private void verifyRoleName(String roleName, HdContextType contextType) {
        List<HdRoleName> hdRoleNames = getByContextType(contextType);
        if (hdRoleNames.stream().noneMatch(role -> role.name().equals(roleName))) {
            throw new IllegalArgumentException(String.format("Invalid %s role name: %s", contextType.getTypeName().toLowerCase(Locale.ROOT), roleName));
        }
    }

    List<CsvUserRole> parseCsvFile(InputStream inputStream) throws IOException {
        List<CsvUserRole> records = new ArrayList<>();
        boolean hasDashboardGroupColumn = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(CSV_DELIMITER)
                     .setSkipHeaderRecord(false)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord csvRecord : csvParser) {
                String[] headerNames = Arrays.stream(csvRecord.values())
                        .map(s -> s.replace("\uFEFF", "").trim()) // Remove BOM character and trim
                        .toArray(String[]::new);
                if (csvRecord.getRecordNumber() == 1L) {
                    hasDashboardGroupColumn = detectHeaders(headerNames);
                    continue;
                }
                CsvUserRole csvUserRole = parseRecord(csvRecord, hasDashboardGroupColumn);
                log.debug("Adding record: {}", csvUserRole);
                records.add(csvUserRole);
            }
        }
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No records found in the CSV file!");
        }
        return records;
    }

    private boolean detectHeaders(String[] headerNames) {
        log.info("Extracted Headers: {}", Arrays.toString(headerNames));
        log.info("Expected Headers (base): {}", Arrays.toString(CSV_HEADERS));
        log.info("Expected Headers (extended): {}", Arrays.toString(CSV_HEADERS_WITH_DASHBOARD_GROUP));
        if (Arrays.equals(headerNames, CSV_HEADERS_WITH_DASHBOARD_GROUP)) {
            return true;
        }
        if (Arrays.equals(headerNames, CSV_HEADERS)) {
            return false;
        }
        throw new IllegalArgumentException("No proper headers found in the CSV file! Should have: " + Arrays.toString(CSV_HEADERS) + " or " + Arrays.toString(CSV_HEADERS_WITH_DASHBOARD_GROUP));
    }

    private CsvUserRole parseRecord(CSVRecord csvRecord, boolean hasDashboardGroupColumn) {
        String email = csvRecord.get(0);
        verifyEmail(email);
        String businessDomainRole = csvRecord.get(1).toUpperCase(Locale.ROOT);
        verifyRoleName(businessDomainRole, HdContextType.BUSINESS_DOMAIN);
        String context = csvRecord.get(2);
        String dataDomainRole = csvRecord.get(3).toUpperCase(Locale.ROOT);
        verifyRoleName(dataDomainRole, HdContextType.DATA_DOMAIN);
        String supersetRoleRaw = csvRecord.get(4);
        log.debug("Superset Roles Raw: {}", supersetRoleRaw);
        List<String> supersetRoles = supersetRoleRaw.isEmpty() ? List.of() : List.of(supersetRoleRaw.split(ROLE_DELIMITER));
        log.debug("Superset roles for email {}: {}", email, supersetRoles);
        List<String> roles = (DATA_DOMAIN_VIEWER.name().equals(dataDomainRole) ||
                DATA_DOMAIN_BUSINESS_SPECIALIST.name().equals(dataDomainRole))
                ? supersetRoles
                : new ArrayList<>();

        List<String> dashboardGroups = List.of();
        if (hasDashboardGroupColumn && csvRecord.size() > 5) {
            String dashboardGroupRaw = csvRecord.get(5);
            dashboardGroups = dashboardGroupRaw.isEmpty() ? List.of() : List.of(dashboardGroupRaw.split(ROLE_DELIMITER));
        }

        return new CsvUserRole(email, businessDomainRole, context, dataDomainRole, roles, dashboardGroups);
    }
}
