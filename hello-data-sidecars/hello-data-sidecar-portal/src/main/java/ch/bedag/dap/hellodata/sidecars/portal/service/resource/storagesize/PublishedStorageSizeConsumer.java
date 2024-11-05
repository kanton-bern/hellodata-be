package ch.bedag.dap.hellodata.sidecars.portal.service.resource.storagesize;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.StorageMonitoringResult;
import ch.bedag.dap.hellodata.portalcommon.monitoring.entity.StorageSizeEntity;
import ch.bedag.dap.hellodata.portalcommon.monitoring.repository.StorageSizeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_STORAGE_MONITORING_RESULT;

@Log4j2
@Service
@Transactional
@AllArgsConstructor
public class PublishedStorageSizeConsumer {
    private final StorageSizeRepository storageSizeRepository;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_STORAGE_MONITORING_RESULT)
    public void getStorageSizeUpdate(StorageMonitoringResult storageMonitoringResult) {
        log.info("Received storage monitoring result {}", storageMonitoringResult);
        StorageSizeEntity storageSizeEntity = new StorageSizeEntity();
        storageSizeEntity.setSizeInfo(storageMonitoringResult);
        storageSizeRepository.save(storageSizeEntity);
    }
}
