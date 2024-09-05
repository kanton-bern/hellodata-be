/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.log.cleanup.service.superset;

import ch.bedag.dap.hellodata.log.cleanup.config.superset.SupersetDynamicDataSource;
import ch.bedag.dap.hellodata.log.cleanup.service.CleanupService;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class SupersetCleanupService implements CleanupService {

    private static final String FIND_LOGS_BEFORE_SQL = "SELECT count(a) FROM logs a WHERE a.dttm <= ?";
    private static final String DELETE_LOGS_BEFORE_SQL = "DELETE FROM logs a WHERE a.dttm <= ?";
    private final SupersetDynamicDataSource supersetDynamicDataSource;

    public void cleanup(LocalDateTime creationDateTime) {
        for (String key : supersetDynamicDataSource.getDataSources().keySet()) {
            DataSource dataSource = supersetDynamicDataSource.getDataSource(key);
            try (Connection connection = dataSource.getConnection()) {
                findAllWithCreationDateTimeBefore(creationDateTime, connection);
                deleteAllWithCreationDateTimeBefore(creationDateTime, connection);
            } catch (SQLException e) {
                log.error("", e);
            }
        }
    }

    private static void deleteAllWithCreationDateTimeBefore(LocalDateTime creationDateTime, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_LOGS_BEFORE_SQL)) {
            statement.setDate(1, Date.valueOf(creationDateTime.toLocalDate()));
            int deletedRows = statement.executeUpdate();
            log.info("Deleted {} superset log entries for {}, schema {}", deletedRows, connection.getCatalog(), connection.getSchema());
        }
    }

    private static void findAllWithCreationDateTimeBefore(LocalDateTime creationDateTime, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_LOGS_BEFORE_SQL)) {
            statement.setDate(1, Date.valueOf(creationDateTime.toLocalDate()));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String found = resultSet.getString(1);
                    log.info("Found {} superset log entries for {}, schema {}", found, connection.getCatalog(), connection.getSchema());
                }
            }
        }
    }
}
