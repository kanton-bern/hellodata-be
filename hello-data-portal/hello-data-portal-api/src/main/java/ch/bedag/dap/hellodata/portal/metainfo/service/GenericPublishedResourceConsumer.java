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

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.portal.metainfo.entity.HdContextEntity;
import ch.bedag.dap.hellodata.portal.metainfo.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.portal.metainfo.repository.HdContextRepository;
import ch.bedag.dap.hellodata.portal.metainfo.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO;

@Log4j2
@Service
@Transactional
@AllArgsConstructor
public class GenericPublishedResourceConsumer {

    private final ResourceRepository resourceRepository;
    private final HdContextRepository contextRepository;

    public MetaInfoResourceEntity persistResource(HdResource hdResource) {
        Optional<MetaInfoResourceEntity> found =
                resourceRepository.findByApiVersionAndModuleTypeAndKindAndInstanceName(hdResource.getApiVersion(), hdResource.getModuleType(), hdResource.getKind(),
                                                                                       hdResource.getInstanceName());
        MetaInfoResourceEntity resource;
        if (found.isPresent()) {
            resource = found.get();
            resource.setMetainfo(hdResource);
            resource.setModifiedDate(LocalDateTime.now());
        } else {
            resource = new MetaInfoResourceEntity();
            resource.setMetainfo(hdResource);
            resource.setApiVersion(hdResource.getApiVersion());
            resource.setModuleType(hdResource.getModuleType());
            resource.setKind(hdResource.getKind());
            resource.setInstanceName(hdResource.getMetadata().instanceName());
        }
        log.info("Resource saved: {}", resource);
        return saveEntity(resource);
    }

    public HdContextEntity attachContext(HdResource hdResource, MetaInfoResourceEntity resource) {
        ModuleType moduleType = hdResource.getModuleType();
        String apiVersion = hdResource.getApiVersion();
        String instanceName = hdResource.getInstanceName();
        Optional<MetaInfoResourceEntity> appInfoEntityFound =
                resourceRepository.findByApiVersionAndModuleTypeAndKindAndInstanceName(apiVersion, moduleType, HELLO_DATA_APP_INFO, instanceName);
        appInfoEntityFound.ifPresent(appInfoEntity -> {
            contextRepository.getByContextKey(appInfoEntity.getContextKey()).ifPresent(context -> resource.setContextKey(context.getContextKey()));
            resourceRepository.save(resource);
        });
        return contextRepository.getByContextKey(resource.getContextKey()).orElse(null);
    }

    public MetaInfoResourceEntity saveEntity(MetaInfoResourceEntity entity) {
        return resourceRepository.save(entity);
    }
}
