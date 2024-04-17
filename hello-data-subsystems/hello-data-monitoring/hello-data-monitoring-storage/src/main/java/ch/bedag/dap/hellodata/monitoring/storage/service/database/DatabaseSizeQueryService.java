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
package ch.bedag.dap.hellodata.monitoring.storage.service.database;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.database.DatabaseSize;
import ch.bedag.dap.hellodata.monitoring.storage.config.database.DynamicDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DatabaseSizeQueryService {

    private final DynamicDataSource dynamicDataSource;

    public List<DatabaseSize> executeDatabaseQueries() {
        log.info("***Checking databases size:");
        List<DatabaseSize> result = new LinkedList<>();
        for (String key : dynamicDataSource.getDataSources().keySet()) {
            DataSource dataSource = dynamicDataSource.getDataSource(key);
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT pg_database_size(?)")) {
                statement.setString(1, connection.getCatalog());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String used = resultSet.getString(1);
                        log.info("Database: " + key + ", Used: " + used);
                        DatabaseSize databaseSize = new DatabaseSize();
                        databaseSize.setUsedBytes(used);
                        databaseSize.setName(key);
                        databaseSize.setTotalAvailableBytes(dynamicDataSource.getTotalAvailableBytes(key));
                        result.add(databaseSize);
                    }
                }
            } catch (SQLException e) {
                log.error("", e);
            }
        }
        log.info("***Finished databases size check\n");
        return result;
    }
}
