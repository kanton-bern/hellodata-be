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

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class ContextsInitializer {
    private final HdContextRepository contextRepository;
    private final HelloDataContextConfig helloDataContextConfig;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initContexts() {
        log.info("/*-/*-Initializing contexts");
        HelloDataContextConfig.BusinessContext businessContextConfig = helloDataContextConfig.getBusinessContext();
        log.info("/*-/*-Initializing business context {}", businessContextConfig.getName());
        saveContext(businessContextConfig.getType(), businessContextConfig.getName(), businessContextConfig.getKey(), false);

        helloDataContextConfig.getContexts().forEach(ctxt -> {
            log.info("/*-/*-Add context {}", ctxt.getName());
            saveContext(ctxt.getType(), ctxt.getName(), ctxt.getKey(), ctxt.isExtra());
        });
    }

    private void saveContext(String contextTypeString, String contextName, String contextKey, boolean extra) {
        HdContextType contextType = HdContextType.findByTypeName(contextTypeString);
        if (contextRepository.getByTypeAndNameAndKey(contextType, contextName, contextKey).isEmpty()) {
            HdContextEntity context = new HdContextEntity();
            context.setType(contextType);
            context.setName(contextName);
            context.setContextKey(contextKey);
            context.setExtra(extra);
            contextRepository.save(context);
        }
    }
}
