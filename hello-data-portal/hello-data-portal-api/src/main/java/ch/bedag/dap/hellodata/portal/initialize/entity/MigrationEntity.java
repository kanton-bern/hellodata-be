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
package ch.bedag.dap.hellodata.portal.initialize.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity to track executed application migrations.
 * Similar to Flyway's schema_history table but for application-level data migrations.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_migration")
public class MigrationEntity {

    @Id
    @Column(nullable = false, unique = true, length = 100)
    private String migrationKey;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 2000)
    private String errorMessage;

    public MigrationEntity(String migrationKey, String description) {
        this.migrationKey = migrationKey;
        this.description = description;
        this.executedAt = LocalDateTime.now();
        this.success = true;
    }

    public static MigrationEntity failed(String migrationKey, String description, String errorMessage) {
        MigrationEntity entity = new MigrationEntity();
        entity.setMigrationKey(migrationKey);
        entity.setDescription(description);
        entity.setExecutedAt(LocalDateTime.now());
        entity.setSuccess(false);
        entity.setErrorMessage(errorMessage);
        return entity;
    }
}
