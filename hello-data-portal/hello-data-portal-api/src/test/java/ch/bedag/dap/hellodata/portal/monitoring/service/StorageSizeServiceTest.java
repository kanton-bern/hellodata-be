package ch.bedag.dap.hellodata.portal.monitoring.service;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.StorageMonitoringResult;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.database.DatabaseSize;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.storage.StorageSize;
import ch.bedag.dap.hellodata.portal.monitoring.data.DatabaseSizeDto;
import ch.bedag.dap.hellodata.portal.monitoring.data.StorageMonitoringResultDto;
import ch.bedag.dap.hellodata.portal.monitoring.data.StorageSizeDto;
import ch.bedag.dap.hellodata.portalcommon.monitoring.entity.StorageSizeEntity;
import ch.bedag.dap.hellodata.portalcommon.monitoring.repository.StorageSizeRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class StorageSizeServiceTest {
    @InjectMocks
    private StorageSizeService storageSizeService;
    @Mock
    private StorageSizeRepository storageSizeRepository;

    @Test
    void toReadableFormat_976_KB() {
        // given
        int bytes = 1_000_000;

        // when
        String readableFormat = storageSizeService.toReadableFormat(bytes);

        // then
        assertEquals("976 KB", readableFormat);
    }

    @Test
    void toReadableFormat_1_GB() {
        // given
        int bytes = 1_073_741_824;

        // when
        String readableFormat = storageSizeService.toReadableFormat(bytes);

        // then
        assertEquals("1 GB", readableFormat);
    }

    @Test
    void toReadableFormat_8_GB() {
        // given
        long bytes = 9_210_984_192L;

        // when
        String readableFormat = storageSizeService.toReadableFormat(bytes);

        // then
        assertEquals("8 GB", readableFormat);
    }

    @Test
    void toReadableFormat_84_GB() {
        // given
        long bytes = 90_210_984_192L;

        // when
        String readableFormat = storageSizeService.toReadableFormat(bytes);

        // then
        assertEquals("84 GB", readableFormat);
    }

    @Test
    void toReadableFormat_838_GB() {
        // given
        long size = 900_210_984_192L;

        // when
        String readableFormat = storageSizeService.toReadableFormat(size);

        // then
        assertEquals("838 GB", readableFormat);
    }

    @Test
    void toReadableFormat_1_TB() {
        // given
        long size = 1_099_511_627_776L;

        // when
        String readableFormat = storageSizeService.toReadableFormat(size);

        // then
        assertEquals("1 TB", readableFormat);
    }

    @Test
    void testGetLatestResult_checkStorageSize() {
        // given
        StorageSizeEntity storageSizeEntity = new StorageSizeEntity();
        LocalDateTime createdDate = LocalDateTime.now();
        storageSizeEntity.setCreatedDate(createdDate);
        StorageMonitoringResult storageMonitoringResult = new StorageMonitoringResult();
        StorageSize storageSize = new StorageSize();
        String storageSizeName = "storage 1";
        storageSize.setName(storageSizeName);
        String storageSizePath = "/path";
        storageSize.setPath(storageSizePath);
        storageSize.setUsedBytes("1000000");
        storageSize.setFreeSpaceBytes("1000000000");
        storageMonitoringResult.setStorageSizes(List.of(storageSize));
        storageMonitoringResult.setDatabaseSizes(List.of());
        storageSizeEntity.setSizeInfo(storageMonitoringResult);
        when(storageSizeRepository.findFirstByOrderByCreatedDateDesc()).thenReturn(Optional.of(storageSizeEntity));
        StorageMonitoringResultDto expectedDto = new StorageMonitoringResultDto();
        expectedDto.setCreatedDate(createdDate);
        StorageSizeDto storageSizeDto = new StorageSizeDto();
        storageSizeDto.setUsedSize("976 KB");
        storageSizeDto.setFreeSpaceSize("953 MB");
        storageSizeDto.setTotalAvailableSize("0 bytes");
        storageSizeDto.setName(storageSizeName);
        storageSizeDto.setPath(storageSizePath);
        expectedDto.setStorageSizes(List.of(storageSizeDto));

        // when
        StorageMonitoringResultDto actualDto = storageSizeService.getLatestResult();

        // then
        verify(storageSizeRepository, times(1)).findFirstByOrderByCreatedDateDesc();
        assertEquals(expectedDto, actualDto);
    }

    @Test
    void testGetLatestResult_checkDatabaseSize() {
        // given
        StorageSizeEntity storageSizeEntity = new StorageSizeEntity();
        LocalDateTime createdDate = LocalDateTime.now();
        storageSizeEntity.setCreatedDate(createdDate);
        StorageMonitoringResult storageMonitoringResult = new StorageMonitoringResult();
        DatabaseSize databaseSize = new DatabaseSize();
        String dbName = "db name";
        databaseSize.setName(dbName);
        databaseSize.setUsedBytes("1000000");
        databaseSize.setTotalAvailableBytes("1000000000");

        storageMonitoringResult.setStorageSizes(List.of());
        storageMonitoringResult.setDatabaseSizes(List.of(databaseSize));
        storageSizeEntity.setSizeInfo(storageMonitoringResult);
        when(storageSizeRepository.findFirstByOrderByCreatedDateDesc()).thenReturn(Optional.of(storageSizeEntity));
        StorageMonitoringResultDto expectedDto = new StorageMonitoringResultDto();
        expectedDto.setCreatedDate(createdDate);
        DatabaseSizeDto databaseSizeDto = new DatabaseSizeDto();
        databaseSizeDto.setName(dbName);
        databaseSizeDto.setUsedSize("976 KB");
        databaseSizeDto.setTotalAvailableSize("953 MB");
        expectedDto.setDatabaseSizes(List.of(databaseSizeDto));

        // when
        StorageMonitoringResultDto actualDto = storageSizeService.getLatestResult();

        // then
        verify(storageSizeRepository, times(1)).findFirstByOrderByCreatedDateDesc();
        assertEquals(expectedDto, actualDto);
    }
}
