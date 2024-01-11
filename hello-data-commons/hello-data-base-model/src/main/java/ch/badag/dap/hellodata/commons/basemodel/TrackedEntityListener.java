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
package ch.badag.dap.hellodata.commons.basemodel;

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TrackedEntityListener {

    @PrePersist
    void onCreate(Object entity) {
        log.debug("On create {}", entity);
        validateEntityClass(entity);
        Trackable trackedEntity = (Trackable) entity;
        if (trackedEntity.getId() == null) {
            trackedEntity.setId(UUID.randomUUID());
        }
        trackedEntity.setCreatedBy(SecurityUtils.getCurrentUsername());
        trackedEntity.setCreatedDate(LocalDateTime.now());
    }

    @PreUpdate
    void onUpdate(Object entity) {
        log.debug("On update {}", entity);
        validateEntityClass(entity);
        Trackable trackedEntity = (Trackable) entity;
        trackedEntity.setModifiedBy(SecurityUtils.getCurrentUsername());
        trackedEntity.setModifiedDate(LocalDateTime.now());
    }

    private void validateEntityClass(Object o) {
        if (!(o instanceof Trackable)) {
            throw new RuntimeException(this.getClass().getSimpleName() + " can only handle classes of type " + Trackable.class.getSimpleName() + "!");
        }
    }
}

