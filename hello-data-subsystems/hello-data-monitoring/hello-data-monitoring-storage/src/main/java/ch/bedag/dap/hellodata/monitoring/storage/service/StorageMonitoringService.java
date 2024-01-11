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
package ch.bedag.dap.hellodata.monitoring.storage.service;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.StorageMonitoringResult;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.database.DatabaseSize;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.storage.StorageSize;
import ch.bedag.dap.hellodata.monitoring.storage.service.database.DatabaseSizeQueryService;
import ch.bedag.dap.hellodata.monitoring.storage.service.storage.StorageSizeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_STORAGE_MONITORING_RESULT;

@Log4j2
@Service
@RequiredArgsConstructor
public class StorageMonitoringService {

    private final DatabaseSizeQueryService databaseSizeQueryService;
    private final StorageSizeService storageSizeService;
    private final NatsSenderService natsSenderService;

    @Scheduled(cron = "${hellodata.monitoring-interval.cron-expression}")
    public void checkSizes() {
        List<DatabaseSize> databaseSizes = databaseSizeQueryService.executeDatabaseQueries();
        List<StorageSize> storageSizes = storageSizeService.checkStorageSize();
        log.info("databaseSizeDtos {}", databaseSizes);
        log.info("storageSizeDtos {}", storageSizes);
        StorageMonitoringResult storageMonitoringResult = new StorageMonitoringResult();
        storageMonitoringResult.setStorageSizes(storageSizes);
        storageMonitoringResult.setDatabaseSizes(databaseSizes);
        natsSenderService.publishMessageToJetStream(UPDATE_STORAGE_MONITORING_RESULT, storageMonitoringResult);
    }
}
