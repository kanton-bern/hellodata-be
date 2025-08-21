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

import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.Pipeline;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineInstance;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineInstanceState;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineResource;
import ch.bedag.dap.hellodata.portal.orchestration.data.PipelineDto;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceTest {

    @InjectMocks
    private OrchestrationService orchestrationService;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private UserRepository userRepository;

    @Test
    public void testGetAllPipelines() {
        // given
        UUID currentUserId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(currentUserId);
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(userEntity);

        UserContextRoleEntity contextRoleEntity = new UserContextRoleEntity();
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(HdRoleName.DATA_DOMAIN_ADMIN);
        roleEntity.setContextType(HdContextType.DATA_DOMAIN);
        contextRoleEntity.setRole(roleEntity);
        contextRoleEntity.setContextKey("CONTEXT_KEY");
        userEntity.setContextRoles(Collections.singleton(contextRoleEntity));

        Pipeline pipeline = new Pipeline();
        pipeline.setId("id");
        pipeline.setActive(true);
        pipeline.setPaused(false);
        pipeline.setFileLocation("/CONTEXT_KEY/");
        pipeline.setLastInstance(new PipelineInstance("id", "123", null, null, PipelineInstanceState.SUCCESS));
        PipelineResource pipelineResource = new PipelineResource("instanceName", List.of(pipeline));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.AIRFLOW, ModuleResourceKind.HELLO_DATA_PIPELINES, PipelineResource.class)).thenReturn(
                Collections.singletonList(pipelineResource));

        // when
        List<PipelineDto> pipelines;
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);
            pipelines = orchestrationService.getAllPipelines();
        }

        // then
        assertEquals(1, pipelines.size());
        assertNotNull(pipelines.get(0));
    }

    @Test
    public void testGetAllPipelinesNoUserId() {
        // given

        List<PipelineDto> pipelines;
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(null);
            // when
            pipelines = orchestrationService.getAllPipelines();
        }

        // then
        assertTrue(pipelines.isEmpty());
    }

    @Test
    public void testGetAllPipelinesNoContextRoles() {
        // given
        UUID currentUserId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(currentUserId);
        userEntity.setContextRoles(Collections.emptySet());
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(userEntity);

        List<PipelineDto> pipelines;
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);
            // when
            pipelines = orchestrationService.getAllPipelines();
        }

        // then
        assertTrue(pipelines.isEmpty());
    }

    @Test
    public void testGetAllPipelinesNoMatchingContextKey() {
        // given
        UUID currentUserId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(currentUserId);
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(userEntity);

        UserContextRoleEntity contextRoleEntity = new UserContextRoleEntity();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(HdRoleName.DATA_DOMAIN_ADMIN);
        roleEntity.setContextType(HdContextType.DATA_DOMAIN);
        contextRoleEntity.setRole(roleEntity);
        contextRoleEntity.setContextKey("DIFFERENT_CONTEXT_KEY");
        userEntity.setContextRoles(Collections.singleton(contextRoleEntity));

        Pipeline pipeline = new Pipeline();
        pipeline.setId("id");
        pipeline.setActive(true);
        pipeline.setPaused(false);
        pipeline.setFileLocation("/CONTEXT_KEY/");
        pipeline.setLastInstance(new PipelineInstance("id", "123", null, null, PipelineInstanceState.SUCCESS));
        PipelineResource pipelineResource = new PipelineResource("instanceName", List.of(pipeline));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.AIRFLOW, ModuleResourceKind.HELLO_DATA_PIPELINES, PipelineResource.class)).thenReturn(
                Collections.singletonList(pipelineResource));

        List<PipelineDto> pipelines;
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);
            // when
            pipelines = orchestrationService.getAllPipelines();
        }

        // then
        assertTrue(pipelines.isEmpty());
    }
}

