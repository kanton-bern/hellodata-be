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
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class StorageSizeService {
    private final HelloDataStorageConfigurationProperties helloDataStorageConfigurationProperties;

    public List<StorageSize> checkStorageSize() {
        log.info("^^^Checking storages size:");
        List<StorageSize> result = new LinkedList<>();
        List<StorageConfigurationProperty> storages = helloDataStorageConfigurationProperties.getStorages();
        try {
            for (StorageConfigurationProperty storage : storages) {
                String path = storage.getPath();
                File folder = new File(path);
                String directorySize = "" + FileUtils.sizeOfDirectory(folder);
                String freeSpace = "" + Files.getFileStore(folder.toPath()).getUsableSpace();
                log.info("Readable folder {} [path: {}] size: {}, free space: {}", storage.getName(), path, directorySize, freeSpace);
                StorageSize storageSize = new StorageSize();
                storageSize.setName(storage.getName());
                storageSize.setPath(path);
                storageSize.setUsedBytes(directorySize);
                storageSize.setFreeSpaceBytes(freeSpace);
                storageSize.setTotalAvailableBytes(storage.getTotalAvailableBytes());
                result.add(storageSize);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error fetching free space in the filesystem", e);
        }
        log.info("^^^Finished storages size check\n");
        return result;
    }
}
