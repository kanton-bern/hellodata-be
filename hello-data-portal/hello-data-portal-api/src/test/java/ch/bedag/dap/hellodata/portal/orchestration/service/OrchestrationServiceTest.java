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
import ch.bedag.dap.hellodata.portal.role.service.AirflowKeycloakRoleService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class OrchestrationServiceTest {

    private OrchestrationService orchestrationService;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AirflowKeycloakRoleService airflowKeycloakRoleService;

    @BeforeEach
    void setUp() {
        orchestrationService = new OrchestrationService(metaInfoResourceService, userRepository, airflowKeycloakRoleService, true, List.of("PROD"), List.of());
    }

    @Test
    void testGetAllPipelines() {
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
    void testGetAllPipelinesNoUserId() {
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
    void testGetAllPipelinesNoContextRoles() {
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
    void testGetAllPipelinesNoMatchingContextKey() {
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

    @Test
    void testGetAllPipelinesShowsOnlyVisibleTagged() {
        // given
        UUID currentUserId = mockUserWithContextKeys(List.of("CONTEXT_KEY"));
        mockAirflowPipelines(
                pipeline("prod_dag", "/CONTEXT_KEY/", List.of("prod", "CONTEXT_KEY")),
                pipeline("dev_dag", "/CONTEXT_KEY/", List.of("DEV", "CONTEXT_KEY")),
                pipeline("untagged_dag", "/CONTEXT_KEY/", null));

        // when
        List<PipelineDto> pipelines = getAllPipelinesAs(currentUserId, true, List.of("PROD"), List.of());

        // then
        assertEquals(List.of("prod_dag"), pipelines.stream().map(PipelineDto::getId).toList());
    }

    @Test
    void testGetAllPipelinesShowsAllWhenDataDomainHasNoVisibleTag() {
        // given
        UUID currentUserId = mockUserWithContextKeys(List.of("CONTEXT_KEY", "OTHER_KEY"));
        mockAirflowPipelines(
                pipeline("prod_dag", "/CONTEXT_KEY/", List.of("PROD")),
                pipeline("dev_dag", "/CONTEXT_KEY/", List.of("DEV")),
                pipeline("other_dag_1", "/OTHER_KEY/", List.of("DEV")),
                pipeline("other_dag_2", "/OTHER_KEY/", null));

        // when
        List<PipelineDto> pipelines = getAllPipelinesAs(currentUserId, true, List.of("PROD"), List.of());

        // then
        assertEquals(List.of("prod_dag", "other_dag_1", "other_dag_2"), pipelines.stream().map(PipelineDto::getId).toList());
    }

    @Test
    void testGetAllPipelinesHidesHiddenTagsEvenWithoutVisibleTags() {
        // given
        UUID currentUserId = mockUserWithContextKeys(List.of("CONTEXT_KEY"));
        mockAirflowPipelines(
                pipeline("prod_dag", "/CONTEXT_KEY/", List.of("PROD")),
                pipeline("dev_dag", "/CONTEXT_KEY/", List.of("dev")),
                pipeline("untagged_dag", "/CONTEXT_KEY/", null));

        // when
        List<PipelineDto> pipelines = getAllPipelinesAs(currentUserId, true, List.of(), List.of("DEV"));

        // then
        assertEquals(List.of("prod_dag", "untagged_dag"), pipelines.stream().map(PipelineDto::getId).toList());
    }

    @Test
    void testGetAllPipelinesHiddenTagWinsOverVisibleTag() {
        // given
        UUID currentUserId = mockUserWithContextKeys(List.of("CONTEXT_KEY"));
        mockAirflowPipelines(
                pipeline("prod_dag", "/CONTEXT_KEY/", List.of("PROD")),
                pipeline("prod_debug_dag", "/CONTEXT_KEY/", List.of("PROD", "DEBUG")),
                pipeline("other_dag", "/CONTEXT_KEY/", List.of("SUSA")));

        // when
        List<PipelineDto> pipelines = getAllPipelinesAs(currentUserId, true, List.of("PROD", "PRODUCTION"), List.of("DEBUG"));

        // then
        assertEquals(List.of("prod_dag"), pipelines.stream().map(PipelineDto::getId).toList());
    }

    @Test
    void testGetAllPipelinesShowsAllWhenNoTagsConfigured() {
        // given
        UUID currentUserId = mockUserWithContextKeys(List.of("CONTEXT_KEY"));
        mockAirflowPipelines(
                pipeline("prod_dag", "/CONTEXT_KEY/", List.of("PROD")),
                pipeline("dev_dag", "/CONTEXT_KEY/", List.of("DEV")));

        // when
        List<PipelineDto> pipelines = getAllPipelinesAs(currentUserId, true, List.of(""), List.of());

        // then
        assertEquals(List.of("prod_dag", "dev_dag"), pipelines.stream().map(PipelineDto::getId).toList());
    }

    @Test
    void testGetAllPipelinesShowsAllWhenTagFilterDisabled() {
        // given
        UUID currentUserId = mockUserWithContextKeys(List.of("CONTEXT_KEY"));
        mockAirflowPipelines(
                pipeline("prod_dag", "/CONTEXT_KEY/", List.of("PROD")),
                pipeline("dev_dag", "/CONTEXT_KEY/", List.of("DEV")));

        // when
        List<PipelineDto> pipelines = getAllPipelinesAs(currentUserId, false, List.of("PROD"), List.of("DEV"));

        // then
        assertEquals(List.of("prod_dag", "dev_dag"), pipelines.stream().map(PipelineDto::getId).toList());
    }

    private UUID mockUserWithContextKeys(List<String> contextKeys) {
        UUID currentUserId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(currentUserId);
        Set<UserContextRoleEntity> contextRoles = new LinkedHashSet<>();
        for (String contextKey : contextKeys) {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setName(HdRoleName.DATA_DOMAIN_VIEWER);
            roleEntity.setContextType(HdContextType.DATA_DOMAIN);
            UserContextRoleEntity contextRoleEntity = new UserContextRoleEntity();
            contextRoleEntity.setRole(roleEntity);
            contextRoleEntity.setContextKey(contextKey);
            contextRoles.add(contextRoleEntity);
        }
        userEntity.setContextRoles(contextRoles);
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(userEntity);
        return currentUserId;
    }

    private void mockAirflowPipelines(Pipeline... pipelines) {
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.AIRFLOW, ModuleResourceKind.HELLO_DATA_PIPELINES, PipelineResource.class)).thenReturn(
                List.of(new PipelineResource("instanceName", List.of(pipelines))));
    }

    private Pipeline pipeline(String id, String fileLocation, List<String> tags) {
        return Pipeline.builder()
                .id(id)
                .fileLocation(fileLocation)
                .tags(tags)
                .lastInstance(new PipelineInstance(id, "123", null, null, PipelineInstanceState.SUCCESS))
                .build();
    }

    private List<PipelineDto> getAllPipelinesAs(UUID currentUserId, boolean tagFilterEnabled, List<String> visibleTags, List<String> hiddenTags) {
        OrchestrationService service = new OrchestrationService(metaInfoResourceService, userRepository, airflowKeycloakRoleService, tagFilterEnabled, visibleTags, hiddenTags);
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);
            return service.getAllPipelines();
        }
    }
}
