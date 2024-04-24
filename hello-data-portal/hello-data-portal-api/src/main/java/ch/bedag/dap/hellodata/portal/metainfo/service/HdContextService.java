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
package ch.bedag.dap.hellodata.portal.metainfo.service;

import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.portal.metainfo.entity.HdContextEntity;
import ch.bedag.dap.hellodata.portal.metainfo.repository.HdContextRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@AllArgsConstructor
public class HdContextService {

    private final HdContextRepository contextRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HdContextEntity saveBusinessContext(HdBusinessContextInfo businessContextInfo) {
        log.debug("Saving business context info {}", businessContextInfo);
        if (businessContextInfo == null || businessContextInfo.getKey() == null) {
            return null;
        }
        HdContextEntity contextForResource;
        HdContextType type = HdContextType.findByTypeName(businessContextInfo.getType());
        String name = businessContextInfo.getName();
        String key = businessContextInfo.getKey();

        HdContextEntity businessContextEntity;
        Optional<HdContextEntity> businessContextEntityFound = contextRepository.getByTypeAndNameAndKey(type, name, key);
        log.debug("Business Context found: {} [by type {}, name {}, key {}]", businessContextEntityFound.isPresent(), type, name, key);
        if (businessContextEntityFound.isEmpty()) {
            HdContextEntity businessContext = new HdContextEntity();
            businessContext.setType(type);
            businessContext.setName(name);
            businessContext.setContextKey(key);
            businessContext.setExtra(businessContext.isExtra());
            businessContextEntity = contextRepository.save(businessContext);
        } else {
            businessContextEntity = businessContextEntityFound.get();
        }
        contextForResource = businessContextEntity;
        HdBusinessContextInfo subContext = businessContextInfo.getSubContext();
        if (subContext != null) {
            String subContextName = subContext.getName();
            HdContextType subContextType = HdContextType.findByTypeName(subContext.getType());
            String subContextKey = subContext.getKey();
            HdContextEntity subContextEntity;
            Optional<HdContextEntity> subContextEntityFound = contextRepository.getByTypeAndNameAndKey(subContextType, subContextName, subContextKey);
            if (subContextEntityFound.isPresent()) {
                subContextEntity = subContextEntityFound.get();
                subContextEntity.setParentContextKey(businessContextEntity.getContextKey());
            } else {
                subContextEntity = new HdContextEntity();
                subContextEntity.setName(subContextName);
                subContextEntity.setType(subContextType);
                subContextEntity.setContextKey(subContextKey);
                subContextEntity.setParentContextKey(businessContextEntity.getContextKey());
            }
            subContextEntity.setExtra(subContext.isExtra());
            log.debug("Saving {} context, the name: {}, is extra? {}", subContextEntity.getType(), subContextEntity.getName(), subContextEntity.isExtra());
            contextRepository.save(subContextEntity);
            contextForResource = subContextEntity;
        }
        return contextForResource;
    }
}
