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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.portal.excel.service.ExcelParserService;
import ch.bedag.dap.hellodata.portal.user.data.BatchUpdateContextRolesForUserDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Service
public class BatchUsersInvitationService {
    private final ExcelParserService excelParserService;
    private final String batchUsersFileLocation;

    public BatchUsersInvitationService(ExcelParserService excelParserService,
                                       @Value("${hello-data.batch-users-file-location}") String batchUsersFileLocation) {
        this.excelParserService = excelParserService;
        this.batchUsersFileLocation = batchUsersFileLocation;
    }

    private void deleteFile(File file) {
        if (file.delete()) {
            log.info("Batch users file successfully deleted: {}", file.getAbsolutePath());
        } else {
            log.error("Failed to delete batch users file: {}", file.getAbsolutePath());
        }
    }

    List<BatchUpdateContextRolesForUserDto> fetchUsersFile(boolean removeFilesAfterFetch) {
        File directory = new File(batchUsersFileLocation);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("Batch users directory does not exist or is not a directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx"));
        if (files == null || files.length == 0) {
            log.warn("No .xlsx files found in directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        List<BatchUpdateContextRolesForUserDto> allUsers = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                List<BatchUpdateContextRolesForUserDto> users = excelParserService.transform(fis);
                allUsers.addAll(users);
                if (removeFilesAfterFetch) {
                    deleteFile(file);
                }
            } catch (IOException e) {
                log.error("Error processing batch users file: {}", file.getAbsolutePath(), e);
                throw new RuntimeException("Error processing batch users file", e);
            }
        }
        return allUsers;
    }
}
