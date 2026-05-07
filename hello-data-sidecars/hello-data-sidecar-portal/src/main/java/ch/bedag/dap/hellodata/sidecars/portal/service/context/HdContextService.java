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
package ch.bedag.dap.hellodata.sidecars.portal.service.context;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@Transactional
@AllArgsConstructor
public class HdContextService {

    private final HdContextRepository contextRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HdContextEntity saveBusinessContext(HdBusinessContextInfo businessContextInfo) {
        log.debug("Saving business context info {}", businessContextInfo);
        if (businessContextInfo == null || businessContextInfo.getKey() == null) {
            return null;
        }
        HdContextType type = HdContextType.findByTypeName(businessContextInfo.getType());
        String name = businessContextInfo.getName();
        String key = businessContextInfo.getKey();

        HdContextEntity businessContextEntity = findOrCreateContext(type, name, key, null, false);
        HdContextEntity contextForResource = businessContextEntity;

        HdBusinessContextInfo subContext = businessContextInfo.getSubContext();
        if (subContext != null) {
            String subContextName = subContext.getName();
            HdContextType subContextType = HdContextType.findByTypeName(subContext.getType());
            String subContextKey = subContext.getKey();
            HdContextEntity subContextEntity = findOrCreateContext(subContextType, subContextName, subContextKey,
                    businessContextEntity.getContextKey(), subContext.isExtra());
            contextForResource = subContextEntity;
        }
        return contextForResource;
    }

    private HdContextEntity findOrCreateContext(HdContextType type, String name, String key, String parentContextKey, boolean extra) {
        Optional<HdContextEntity> existing = contextRepository.getByContextKey(key);
        if (existing.isPresent()) {
            HdContextEntity entity = existing.get();
            boolean updated = false;
            if (parentContextKey != null && !parentContextKey.equals(entity.getParentContextKey())) {
                entity.setParentContextKey(parentContextKey);
                updated = true;
            }
            if (entity.isExtra() != extra) {
                entity.setExtra(extra);
                updated = true;
            }
            if (updated) {
                entity = contextRepository.save(entity);
            }
            return entity;
        }

        // Use native upsert to handle concurrent inserts safely
        UUID id = UUID.randomUUID();
        Query upsertQuery = entityManager.createNativeQuery(
                "INSERT INTO context (id, context_key, name, type, parent_key, extra, created_by, created_date, modified_by, modified_date) " +
                        "VALUES (:id, :key, :name, :type, :parentKey, :extra, 'system', now(), 'system', now()) " +
                        "ON CONFLICT (context_key) DO NOTHING");
        upsertQuery.setParameter("id", id);
        upsertQuery.setParameter("key", key);
        upsertQuery.setParameter("name", name);
        upsertQuery.setParameter("type", type.name());
        upsertQuery.setParameter("parentKey", parentContextKey);
        upsertQuery.setParameter("extra", extra);
        upsertQuery.executeUpdate();

        // Fetch the entity (either we just inserted it, or it already existed)
        entityManager.clear();
        return contextRepository.getByContextKey(key)
                .orElseThrow(() -> new IllegalStateException("Context with key " + key + " not found after upsert"));
    }
}
