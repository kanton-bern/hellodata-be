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
package ch.bedag.dap.hellodata.portal.monitoring.service;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.StorageMonitoringResult;
import ch.bedag.dap.hellodata.portal.monitoring.data.StorageMonitoringResultDto;
import ch.bedag.dap.hellodata.portal.monitoring.entity.StorageSizeEntity;
import ch.bedag.dap.hellodata.portal.monitoring.repository.StorageSizeRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_STORAGE_MONITORING_RESULT;

@Log4j2
@Service
@RequiredArgsConstructor
public class StorageSizeService {
    private final StorageSizeRepository storageSizeRepository;
    private final ModelMapper modelMapper;

    @JetStreamSubscribe(event = UPDATE_STORAGE_MONITORING_RESULT)
    public CompletableFuture<Void> getStorageSizeUpdate(StorageMonitoringResult storageMonitoringResult) {
        log.info("Received storage monitoring result {}", storageMonitoringResult);
        StorageSizeEntity storageSizeEntity = new StorageSizeEntity();
        storageSizeEntity.setSizeInfo(storageMonitoringResult);
        storageSizeRepository.save(storageSizeEntity);
        return null;
    }

    @Transactional
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void removeOldEntries() {
        storageSizeRepository.deleteAllByCreatedDateBefore(LocalDateTime.now().minusDays(14));
    }

    @Transactional(readOnly = true)
    public StorageMonitoringResultDto getLatestResult() {
        Optional<StorageSizeEntity> firstByOrderByCreatedDateDesc = storageSizeRepository.findFirstByOrderByCreatedDateDesc();
        if (firstByOrderByCreatedDateDesc.isPresent()) {
            StorageSizeEntity storageSizeEntity = firstByOrderByCreatedDateDesc.get();
            StorageMonitoringResultDto dto = modelMapper.map(storageSizeEntity.getSizeInfo(), StorageMonitoringResultDto.class);
            dto.setCreatedDate(storageSizeEntity.getCreatedDate());
            return dto;
        }
        return null;
    }
}
