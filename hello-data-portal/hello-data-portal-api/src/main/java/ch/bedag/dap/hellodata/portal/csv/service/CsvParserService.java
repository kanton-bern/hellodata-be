package ch.bedag.dap.hellodata.portal.csv.service;

import ch.bedag.dap.hellodata.portal.csv.data.CsvUserRole;
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
import java.util.ArrayList;
import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName.DATA_DOMAIN_VIEWER;

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

    public List<CsvUserRole> parseCsvFile(InputStream inputStream) {
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
                String context = csvRecord.get(CONTEXT);
                String dataDomainRole = csvRecord.get(DATA_DOMAIN_ROLE);
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
