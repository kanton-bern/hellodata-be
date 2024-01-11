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
package ch.bedag.dap.hellodata.log.cleanup.service.cloudbeaver;

import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverAuthAttemptEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverAuthAttemptInfoEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverAuthTokenEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverSessionEntity;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverAuthAttemptInfoTokenRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverAuthAttemptRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverAuthTokenRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverSessionRepository;
import ch.bedag.dap.hellodata.log.cleanup.service.CleanupService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2
public class CloudbeaverCleanupService implements CleanupService {

    private final CloudBeaverSessionRepository cloudBeaverSessionRepository;
    private final CloudBeaverAuthTokenRepository cloudBeaverAuthTokenRepository;
    private final CloudBeaverAuthAttemptRepository cloudBeaverAuthAttemptRepository;
    private final CloudBeaverAuthAttemptInfoTokenRepository cloudBeaverAuthAttemptInfoTokenRepository;

    public CloudbeaverCleanupService(CloudBeaverSessionRepository cloudBeaverSessionRepository, CloudBeaverAuthTokenRepository cloudBeaverAuthTokenRepository,
                                     CloudBeaverAuthAttemptRepository cloudBeaverAuthAttemptRepository, CloudBeaverAuthAttemptInfoTokenRepository cloudBeaverAuthAttemptInfoTokenRepository) {
        this.cloudBeaverSessionRepository = cloudBeaverSessionRepository;
        this.cloudBeaverAuthTokenRepository = cloudBeaverAuthTokenRepository;
        this.cloudBeaverAuthAttemptRepository = cloudBeaverAuthAttemptRepository;
        this.cloudBeaverAuthAttemptInfoTokenRepository = cloudBeaverAuthAttemptInfoTokenRepository;
    }

    @Override
    @Transactional("cloudbeaverTransactionManager")
    public void cleanup(LocalDateTime creationDateTime) {
        handleCloudBeaver(creationDateTime);
    }

    private void handleCloudBeaver(LocalDateTime creationDateTime) {
        List<CloudbeaverAuthTokenEntity> cloudBeaverAuthTokenEntries = cloudBeaverAuthTokenRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Found {} cloudbeaver auth-token entries.", cloudBeaverAuthTokenEntries.size());

        int deletedAuthTokenEntries = cloudBeaverAuthTokenRepository.removeAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Deleted {} cloudbeaver auth-token entries.", deletedAuthTokenEntries);

        List<CloudbeaverAuthAttemptEntity> cloudBeaverAuthAttemptEntries = cloudBeaverAuthAttemptRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Found {} cloudbeaver auth-attempt entries.", cloudBeaverAuthAttemptEntries.size());

        int deletedAuthAttemptEntries = cloudBeaverAuthAttemptRepository.removeAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Deleted {} cloudbeaver auth-attempt entries.", deletedAuthAttemptEntries);

        List<CloudbeaverAuthAttemptInfoEntity> cloudBeaverAuthAttemptInfoEntries = cloudBeaverAuthAttemptInfoTokenRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Found {} cloudbeaver auth-attempt-info entries.", cloudBeaverAuthAttemptInfoEntries.size());

        int deletedAuthAttemptInfoEntries = cloudBeaverAuthAttemptInfoTokenRepository.removeAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Deleted {} cloudbeaver auth-attempt-info entries.", deletedAuthAttemptInfoEntries);

        List<CloudbeaverSessionEntity> cloudbeaverSessionEntries = cloudBeaverSessionRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Found {} cloudbeaver session entries.", cloudbeaverSessionEntries.size());

        int deletedSessionEntries = cloudBeaverSessionRepository.removeAllWithCreationDateTimeBefore(creationDateTime);
        log.info("Deleted {} cloudbeaver session entries.", deletedSessionEntries);

    }
}
