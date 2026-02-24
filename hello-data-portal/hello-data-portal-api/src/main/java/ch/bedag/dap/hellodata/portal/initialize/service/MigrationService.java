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
package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.portal.initialize.entity.MigrationEntity;
import ch.bedag.dap.hellodata.portal.initialize.repository.MigrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage application-level data migrations.
 * Tracks which migrations have been executed to ensure they run exactly once.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final MigrationRepository migrationRepository;

    /**
     * Checks if a migration has already been successfully executed.
     *
     * @param migrationKey unique identifier for the migration
     * @return true if migration was already executed successfully
     */
    @Transactional(readOnly = true)
    public boolean isMigrationCompleted(String migrationKey) {
        return migrationRepository.existsByMigrationKeyAndSuccessTrue(migrationKey);
    }

    /**
     * Records a successful migration execution.
     *
     * @param migrationKey unique identifier for the migration
     * @param description  human-readable description of what the migration does
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMigrationSuccess(String migrationKey, String description) {
        if (migrationRepository.existsById(migrationKey)) {
            MigrationEntity existing = migrationRepository.findById(migrationKey).orElseThrow();
            existing.setSuccess(true);
            existing.setErrorMessage(null);
            migrationRepository.save(existing);
        } else {
            migrationRepository.save(new MigrationEntity(migrationKey, description));
        }
        log.info("Migration '{}' completed successfully", migrationKey);
    }

    /**
     * Records a failed migration execution.
     *
     * @param migrationKey unique identifier for the migration
     * @param description  human-readable description of what the migration does
     * @param errorMessage the error message from the failure
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMigrationFailure(String migrationKey, String description, String errorMessage) {
        if (migrationRepository.existsById(migrationKey)) {
            MigrationEntity existing = migrationRepository.findById(migrationKey).orElseThrow();
            existing.setSuccess(false);
            existing.setErrorMessage(errorMessage);
            migrationRepository.save(existing);
        } else {
            migrationRepository.save(MigrationEntity.failed(migrationKey, description, errorMessage));
        }
        log.error("Migration '{}' failed: {}", migrationKey, errorMessage);
    }
}
