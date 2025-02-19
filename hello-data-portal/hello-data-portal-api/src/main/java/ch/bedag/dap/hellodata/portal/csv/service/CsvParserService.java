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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName.DATA_DOMAIN_VIEWER;
import static ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName.getByContextType;

@Log4j2
@Service
@RequiredArgsConstructor
public class CsvParserService {

    private static final String EMAIL = "email";
    private static final String BUSINESS_DOMAIN_ROLE = "businessDomainRole";
    private static final String CONTEXT = "context";
    private static final String DATA_DOMAIN_ROLE = "dataDomainRole";
    private static final String SUPERSET_ROLE = "supersetRole";
    private static final char CSV_DELIMITER = ';';
    private static final String ROLE_DELIMITER = ",";

    public List<BatchUpdateContextRolesForUserDto> transform(InputStream csvStream) {
        List<CsvUserRole> parsedData = parseCsvFile(csvStream);
        Map<String, BatchUpdateContextRolesForUserDto> usersMap = new LinkedHashMap<>();

        for (CsvUserRole row : parsedData) {
            String email = row.email();
            String businessDomainRole = row.businessDomainRole();
            String dataDomain = row.context();
            String dataDomainRole = row.dataDomainRole();
            List<String> supersetRoles = row.supersetRoles();

            usersMap.putIfAbsent(email, new BatchUpdateContextRolesForUserDto());
            BatchUpdateContextRolesForUserDto userDto = usersMap.get(email);

            Map<String, List<ModuleRoleNames>> contextToModuleRoleNamesMap = userDto.getContextToModuleRoleNamesMap();
            contextToModuleRoleNamesMap.computeIfAbsent(dataDomain, k -> new ArrayList<>()).add(new ModuleRoleNames(ModuleType.SUPERSET, supersetRoles));

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

    private void verifyRoleName(String roleName, HdContextType contextType) {
        List<HdRoleName> hdRoleNames = getByContextType(contextType);
        if (hdRoleNames.stream().noneMatch(role -> role.name().equals(roleName))) {
            throw new RuntimeException(String.format("Invalid %s role name: %s", contextType.getTypeName().toLowerCase(Locale.ROOT), roleName));
        }
    }

    List<CsvUserRole> parseCsvFile(InputStream inputStream) {
        List<CsvUserRole> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

             CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(CSV_DELIMITER)
                     .setHeader(EMAIL, BUSINESS_DOMAIN_ROLE, CONTEXT, DATA_DOMAIN_ROLE, SUPERSET_ROLE)
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord csvRecord : csvParser) {
                String email = csvRecord.get(EMAIL);
                String businessDomainRole = csvRecord.get(BUSINESS_DOMAIN_ROLE);
                verifyRoleName(businessDomainRole, HdContextType.BUSINESS_DOMAIN);
                String context = csvRecord.get(CONTEXT);
                String dataDomainRole = csvRecord.get(DATA_DOMAIN_ROLE);
                verifyRoleName(dataDomainRole, HdContextType.DATA_DOMAIN);
                String supersetRoleRaw = csvRecord.get(SUPERSET_ROLE);

                // Convert comma-separated Superset roles into a List
                List<String> supersetRoles = supersetRoleRaw.isEmpty() ? List.of() : List.of(supersetRoleRaw.split(ROLE_DELIMITER));

                // Ensure supersetRoles is empty if dataDomainRole is not "DATA_DOMAIN_VIEWER"
                List<String> roles = DATA_DOMAIN_VIEWER.name().equals(dataDomainRole) ? supersetRoles : new ArrayList<>();
                CsvUserRole record = new CsvUserRole(email, businessDomainRole, context, dataDomainRole, roles);

                records.add(record);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }

        return records;
    }
}
