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
package ch.bedag.dap.hellodata.monitoring.storage.service.storage;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.storage.StorageSize;
import ch.bedag.dap.hellodata.monitoring.storage.config.HelloDataStorageConfigurationProperties;
import ch.bedag.dap.hellodata.monitoring.storage.config.storage.StorageConfigurationProperty;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class StorageSizeServiceTest {

    @Mock
    private HelloDataStorageConfigurationProperties configProperties;

    @InjectMocks
    private StorageSizeService storageSizeService;

    @Test
    void testCheckStorageSize() throws IOException {
        // given
        StorageConfigurationProperty storageProperty1 = new StorageConfigurationProperty();
        storageProperty1.setName("Test Storage 1");
        storageProperty1.setPath("/test/path1");
        when(configProperties.getStorages()).thenReturn(List.of(storageProperty1));
        File mockedFile1 = mock(File.class);
        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class);
             MockedStatic<FileSystemUtils> fileSystemUtilsMockedStatic = Mockito.mockStatic(FileSystemUtils.class)) {
            when(FileUtils.sizeOfDirectory(any())).thenReturn(1000L);
            when(FileSystemUtils.freeSpaceKb("/test/path1")).thenReturn(1024L);
            // when
            List<StorageSize> result = storageSizeService.checkStorageSize();

            // then
            assertEquals(1, result.size());
            StorageSize storageSize1 = result.get(0);
            assertEquals("Test Storage 1", storageSize1.getName());
            assertEquals("/test/path1", storageSize1.getPath());
            assertEquals("1 kB", storageSize1.getSize());
            assertEquals("1 MB", storageSize1.getFreeSpace());
            verify(configProperties, times(1)).getStorages();
        }
    }
}
