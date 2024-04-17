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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class DatabaseSizeQueryServiceTest {

    @Mock
    private DynamicDataSource dynamicDataSource;

    @InjectMocks
    private DatabaseSizeQueryService databaseSizeQueryService;

    @Test
    void testExecuteDatabaseQueries() throws SQLException {
        // given
        Connection mockedConnection = mock(Connection.class);
        PreparedStatement mockedStatement = mock(PreparedStatement.class);
        ResultSet mockedResultSet = mock(ResultSet.class);
        DataSource mockedDataSource = mock(DataSource.class);
        String totalAvailableBytes = "3000000";

        Map<String, DynamicDataSource.DataSourceWrapper> dataSources = new HashMap<>();
        dataSources.put("testDB1", new DynamicDataSource.DataSourceWrapper(mockedDataSource, totalAvailableBytes));
        when(dynamicDataSource.getDataSources()).thenReturn(dataSources);
        when(dynamicDataSource.getDataSource("testDB1")).thenReturn(mockedDataSource);

        when(mockedDataSource.getConnection()).thenReturn(mockedConnection);
        when(mockedConnection.prepareStatement(anyString())).thenReturn(mockedStatement);
        when(mockedStatement.executeQuery()).thenReturn(mockedResultSet);
        when(mockedResultSet.next()).thenReturn(true, false); // Simulate result set behavior
        when(mockedResultSet.getString(1)).thenReturn("100 MB"); // Mock database sizes

        // when
        List<DatabaseSize> result = databaseSizeQueryService.executeDatabaseQueries();

        // then
        assertEquals(1, result.size());

        DatabaseSize databaseSize1 = result.get(0);
        assertEquals("testDB1", databaseSize1.getName());
        assertEquals("100 MB", databaseSize1.getUsedBytes());

        verify(dynamicDataSource, times(1)).getDataSources();
        verify(dynamicDataSource, times(1)).getDataSource(anyString());
        verify(mockedDataSource, times(1)).getConnection();
        verify(mockedConnection, times(1)).prepareStatement(anyString());
        verify(mockedStatement, times(1)).executeQuery();
        verify(mockedResultSet, times(1)).next();
        verify(mockedResultSet, times(1)).getString(1);
    }
}

