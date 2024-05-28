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
package ch.bedag.dap.hellodata.portal.orchestration.service;

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineResource;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.orchestration.data.PipelineDto;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@AllArgsConstructor
public class OrchestrationService {
    private final MetaInfoResourceService metaInfoResourceService;
    private final UserRepository userRepository;

    public List<PipelineDto> getAllPipelines() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            UserEntity userEntity = userRepository.getByIdOrAuthId(currentUserId.toString());
            List<String> contextKeysFromContextRoles = userEntity.getContextRoles()
                                                                 .stream()
                                                                 .filter(contextRole -> contextRole.getRole().getName() != HdRoleName.NONE)
                                                                 .map(UserContextRoleEntity::getContextKey)
                                                                 .toList();
            log.info("Found {} context keys for user {}", contextKeysFromContextRoles, userEntity.getEmail());
            List<PipelineResource> pipelines =
                    metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.AIRFLOW, ModuleResourceKind.HELLO_DATA_PIPELINES, PipelineResource.class);
            return filterByPath(pipelines, contextKeysFromContextRoles);
        }
        return Collections.emptyList();
    }

    private List<PipelineDto> filterByPath(List<PipelineResource> pipelines, List<String> contextKeysFromContextRoles) {
        List<PipelineDto> result = pipelines.stream().flatMap((pipelineResource -> pipelineResource.getData().stream())).map(pipeline -> {
            log.info("Checking pipeline {}", pipeline);
            String contextKeyForDag = contextKeysFromContextRoles.stream()
                                                                 .filter(contextKey -> pipeline.getFileLocation().toLowerCase().contains("/" + contextKey.toLowerCase() + "/"))
                                                                 .findFirst()
                                                                 .orElse(null);
            log.info("Found context key for dag {}", contextKeyForDag);
            if (contextKeyForDag != null) {
                return PipelineDto.fromPipeline(pipeline, contextKeyForDag);
            }
            return null;
        }).filter(Objects::nonNull).toList();
        log.info("Filtered pipelines {}", result);
        return result;
    }
}
