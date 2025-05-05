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
package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.HellodataAuthenticationToken;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardWithMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.repository.DashboardMetadataRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private DashboardMetadataRepository dashboardMetadataRepository;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private HdContextRepository contextRepository;

    private final String email = "test@bedag.ch";
    private final String instanceName = "bd01";
    private final int subsystemId = 1;

    @BeforeEach
    public void initSecurityContext() {
        HellodataAuthenticationToken hellodataAuthenticationToken = new HellodataAuthenticationToken(UUID.randomUUID(), "admin", "user", email, true, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(hellodataAuthenticationToken);
    }

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateDashboard_asAdminUserWithAdminRoles_shouldAllowCreateOrUpdate() {
        //Given
        SubsystemUser subsystemUser = createAdminUser();
        UserResource userResources = createUserResources(subsystemUser);
        configureBaseMocks(userResources, createDashboardResource());
        given(dashboardMetadataRepository.findBySubsystemIdAndInstanceName(subsystemId, instanceName)).willReturn(Optional.empty());

        //When
        dashboardService.updateDashboard(instanceName, subsystemId, new UpdateSupersetDashboardMetadataDto());

        //Then
        verify(dashboardMetadataRepository, times(1)).findBySubsystemIdAndInstanceName(subsystemId, instanceName);
    }

    @Test
    void updateDashboard_asViewer_shouldNotAllowCreateOrUpdate() {
        //Given
        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        configureBaseMocks(userResources, createDashboardResource());

        //When
        Throwable exception =
                assertThrows(ResponseStatusException.class, () -> dashboardService.updateDashboard(instanceName, subsystemId, new UpdateSupersetDashboardMetadataDto()));

        //Then
        assertTrue(exception.getMessage().contains("User is not allowed to update dashboard metadata"));
    }

    @Test
    void updateDashboard_asEditor_shouldAllowCreateOrUpdate() {
        //Given
        SubsystemUser subsystemUser = createEditorUser();
        UserResource userResources = createUserResources(subsystemUser);
        configureBaseMocks(userResources, createDashboardResource());
        given(dashboardMetadataRepository.findBySubsystemIdAndInstanceName(subsystemId, instanceName)).willReturn(Optional.empty());

        //When
        dashboardService.updateDashboard(instanceName, subsystemId, new UpdateSupersetDashboardMetadataDto());
        //Then
        verify(dashboardMetadataRepository, times(1)).findBySubsystemIdAndInstanceName(subsystemId, instanceName);
    }

    @Test
    void fetchMyDashboards_asEditor_shouldReturnAllPublishedDashboards() {
        //Given
        DashboardResource dashboardResource = createDashboardResource();
        SubsystemUser subsystemUser = createEditorUser();
        UserResource userResources = createUserResources(subsystemUser);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);

        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).hasSize(dashboardResource.getData().size());
        assertThat(supersetDashboardDtos.stream()
                                        .allMatch(d -> d instanceof SupersetDashboardWithMetadataDto && ((SupersetDashboardWithMetadataDto) d).isCurrentUserEditor())).isTrue();
    }

    @Test
    void fetchMyDashboards_asViewerWithOneCorrespondingDashboardRole_shouldReturnOneDashboard() {
        //Given
        DashboardResource dashboardResource = createDashboardResource(true);
        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);

        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).hasSize(1);
        assertThat(supersetDashboardDtos.stream()
                                        .allMatch(d -> d instanceof SupersetDashboardWithMetadataDto && ((SupersetDashboardWithMetadataDto) d).isCurrentUserViewer())).isTrue();
    }

    @Test
    void fetchMyDashboards_asViewerWithoutCorrespondingDashboardRole_shouldReturnEmptyList() {
        //Given
        DashboardResource dashboardResource = createDashboardResource();
        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);

        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).isEmpty();
    }

    @Test
    public void fetchMyDashboards_HELLODATA_2159() throws JsonProcessingException {
        //Given
        String data = """
            {
                        "apiVersion": "v1",
                        "data": [
                            {
                                "certification_details": null,
                                "certified_by": null,
                                "changed_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "changed_by_name": "User User",
                                "changed_by_url": null,
                                "changed_on_delta_humanized": "21 days ago",
                                "changed_on_utc": "2025-03-31T18:54:41.780549+0000",
                                "created_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 1,
                                    "last_login": null,
                                    "last_name": "Neidhart",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "created_on_delta_humanized": "4 months ago",
                                "dashboard_title": "Operative Übersicht: Auslastung und Vollzugstage im Fokus",
                                "id": 1,
                                "published": true,
                                "is_managed_externally": false,
                                "roles": [
                                    {
                                        "id": 8,
                                        "name": "BI_VIEWER"
                                    },
                                    {
                                        "id": 7,
                                        "name": "BI_EDITOR"
                                    },
                                    {
                                        "id": 6,
                                        "name": "BI_ADMIN"
                                    },
                                    {
                                        "id": 13,
                                        "name": "D_eingewiesenen-personen-insights_5"
                                    },
                                    {
                                        "id": 12,
                                        "name": "D_auslastung-und-vollzugstage-im-fokus_4"
                                    },
                                    {
                                        "id": 16,
                                        "name": "D_finanzergebnis_8"
                                    }
                                ],
                                "slug": null,
                                "status": "published",
                                "thumbnail_url": "/api/v1/dashboard/1/thumbnail/9d9aeb185e14cde1b230601206ebe279/",
                                "url": "/superset/dashboard/1/"
                            },
                            {
                                "certification_details": "",
                                "certified_by": "",
                                "changed_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "changed_by_name": "User User",
                                "changed_by_url": null,
                                "changed_on_delta_humanized": "22 days ago",
                                "changed_on_utc": "2025-03-30T18:53:16.494447+0000",
                                "created_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 20,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "created_on_delta_humanized": "a month ago",
                                "dashboard_title": "Finanzergebnis",
                                "published": true,
                                "id": 8,
                                "is_managed_externally": false,
                                "roles": [
                                    {
                                        "id": 13,
                                        "name": "D_eingewiesenen-personen-insights_5"
                                    },
                                    {
                                        "id": 12,
                                        "name": "D_auslastung-und-vollzugstage-im-fokus_4"
                                    },
                                    {
                                        "id": 7,
                                        "name": "BI_EDITOR"
                                    },
                                    {
                                        "id": 16,
                                        "name": "D_finanzergebnis_8"
                                    },
                                    {
                                        "id": 8,
                                        "name": "BI_VIEWER"
                                    },
                                    {
                                        "id": 6,
                                        "name": "BI_ADMIN"
                                    }
                                ],
                                "slug": null,
                                "status": "published",
                                "thumbnail_url": "/api/v1/dashboard/8/thumbnail/31ed3f5f85f3216db524a7b869abca25/",
                                "url": "/superset/dashboard/8/"
                            },
                            {
                                "certification_details": "",
                                "certified_by": "",
                                "changed_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "changed_by_name": "User User",
                                "changed_by_url": null,
                                "changed_on_delta_humanized": "24 days ago",
                                "changed_on_utc": "2025-03-28T17:21:57.249952+0000",
                                "created_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 20,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "created_on_delta_humanized": "a month ago",
                                "css": "",
                                "dashboard_title": "Investitionen",
                                "id": 7,
                                "is_managed_externally": false,
                                "published": true,
                                "roles": [
                                    {
                                        "id": 15,
                                        "name": "D_investitionen_7"
                                    },
                                    {
                                        "id": 13,
                                        "name": "D_eingewiesenen-personen-insights_5"
                                    },
                                    {
                                        "id": 6,
                                        "name": "BI_ADMIN"
                                    },
                                    {
                                        "id": 12,
                                        "name": "D_auslastung-und-vollzugstage-im-fokus_4"
                                    },
                                    {
                                        "id": 8,
                                        "name": "BI_VIEWER"
                                    },
                                    {
                                        "id": 7,
                                        "name": "BI_EDITOR"
                                    }
                                ],
                                "slug": null,
                                "status": "published",
                                "thumbnail_url": "/api/v1/dashboard/7/thumbnail/8206123f4985393fb5575a84b78cbfce/",
                                "url": "/superset/dashboard/7/"
                            },
                            {
                                "certification_details": "",
                                "certified_by": "",
                                "changed_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "changed_by_name": "User User",
                                "changed_by_url": null,
                                "changed_on_delta_humanized": "24 days ago",
                                "changed_on_utc": "2025-03-28T15:52:53.634831+0000",
                                "created_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 20,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "created_on_delta_humanized": "a month ago",
                                "dashboard_title": "Zeitguthaben",
                                "id": 10,
                                "is_managed_externally": false,
                                "published": true,
                                "roles": [
                                    {
                                        "id": 18,
                                        "name": "D_zeitguthaben_10"
                                    },
                                    {
                                        "id": 13,
                                        "name": "D_eingewiesenen-personen-insights_5"
                                    },
                                    {
                                        "id": 8,
                                        "name": "BI_VIEWER"
                                    },
                                    {
                                        "id": 12,
                                        "name": "D_auslastung-und-vollzugstage-im-fokus_4"
                                    },
                                    {
                                        "id": 7,
                                        "name": "BI_EDITOR"
                                    },
                                    {
                                        "id": 6,
                                        "name": "BI_ADMIN"
                                    }
                                ],
                                "slug": null,
                                "status": "published",
                                "thumbnail_url": "/api/v1/dashboard/10/thumbnail/00e1abdea313d895fcaba0d46db471ea/",
                                "url": "/superset/dashboard/10/"
                            },
                            {
                                "certification_details": null,
                                "certified_by": null,
                                "changed_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "changed_by_name": "User User",
                                "changed_by_url": null,
                                "changed_on_delta_humanized": "25 days ago",
                                "changed_on_utc": "2025-03-28T08:07:38.858364+0000",
                                "created_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 20,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "created_on_delta_humanized": "a month ago",
                                "dashboard_title": "Eingewiesenen Personen Insights",
                                "id": 5,
                                "is_managed_externally": false,
                                "published": true,
                                "roles": [
                                    {
                                        "id": 15,
                                        "name": "D_investitionen_7"
                                    },
                                    {
                                        "id": 1,
                                        "name": "Admin"
                                    },
                                    {
                                        "id": 8,
                                        "name": "BI_VIEWER"
                                    },
                                    {
                                        "id": 12,
                                        "name": "D_auslastung-und-vollzugstage-im-fokus_4"
                                    },
                                    {
                                        "id": 7,
                                        "name": "BI_EDITOR"
                                    },
                                    {
                                        "id": 9,
                                        "name": "D_operative-ubersicht-auslastung-und-vollzugstage-i_1"
                                    }
                                ],
                                "slug": "EPC",
                                "status": "published",
                                "thumbnail_url": "/api/v1/dashboard/5/thumbnail/41d73d94f8e7ba44178ce687b8ee2851/",
                                "url": "/superset/dashboard/EPC/"
                            },
                            {
                                "certification_details": null,
                                "certified_by": null,
                                "changed_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "changed_by_name": "User User",
                                "changed_by_url": null,
                                "changed_on_delta_humanized": "25 days ago",
                                "changed_on_utc": "2025-03-28T08:05:37.196278+0000",
                                "created_by": {
                                    "active": false,
                                    "changed_by": null,
                                    "changed_on": null,
                                    "created_by": null,
                                    "created_on": null,
                                    "email": null,
                                    "fail_login_count": 0,
                                    "first_name": "User",
                                    "id": 14,
                                    "last_login": null,
                                    "last_name": "User",
                                    "login_count": 0,
                                    "roles": null,
                                    "username": null
                                },
                                "created_on_delta_humanized": "a month ago",
                                "dashboard_title": "Auslastung und Vollzugstage im Fokus",
                                "id": 4,
                                "is_managed_externally": false,
                                "published": true,
                                "roles": [
                                    {
                                        "id": 16,
                                        "name": "D_finanzergebnis_8"
                                    },
                                    {
                                        "id": 6,
                                        "name": "BI_ADMIN"
                                    },
                                    {
                                        "id": 8,
                                        "name": "BI_VIEWER"
                                    }
                                ],
                                "slug": "abc",
                                "status": "published",
                                "thumbnail_url": "/api/v1/dashboard/4/thumbnail/db8ea793888ecb8e65bbe1aeadae6382/",
                                "url": "/superset/dashboard/abc/"
                            }
                        ],
                        "instanceName": "bd01",
                        "kind": "hellodata/Dashboards",
                        "metadata": {
                            "instanceName": "bd01",
                            "labels": {
                                "hellodata/module": "superset"
                            },
                            "namespace": "test"
                        },
                        "moduleType": "SUPERSET"
                    }
        """;

        ObjectMapper objectMapper = new ObjectMapper();
        DashboardResource dashboardResource = objectMapper.readValue(data, DashboardResource.class);

        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);
        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).hasSize(6);
    }

    private void configureBaseMocks(UserResource userResources, DashboardResource dashboardResource) {
        given(metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_USERS, UserResource.class)).willReturn(
                userResources);
        given(metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_DASHBOARDS,
                                                                          DashboardResource.class)).willReturn(dashboardResource);
    }

    @NotNull
    private UserResource createUserResources(SubsystemUser subsystemUser) {
        return new UserResource(ModuleType.SUPERSET, instanceName, "", List.of(subsystemUser));
    }

    @NotNull
    private SubsystemUser createAdminUser() {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail(email);
        SubsystemRole adminRole = createSupersetRole(1, SlugifyUtil.BI_ADMIN_ROLE_NAME);
        subsystemUser.setRoles(List.of(adminRole));
        return subsystemUser;
    }

    @NotNull
    private SubsystemUser createViewerUser() {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail(email);
        SubsystemRole viewerRole = createSupersetRole(1, SlugifyUtil.BI_VIEWER_ROLE_NAME);
        SubsystemRole dashboardRole = createSupersetRole(2, "D_dashboard-2"); // Specific Dashboard-Role
        subsystemUser.setRoles(List.of(viewerRole, dashboardRole));
        return subsystemUser;
    }

    @NotNull
    private SubsystemUser createEditorUser() {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail(email);
        SubsystemRole editorRole = createSupersetRole(1, SlugifyUtil.BI_EDITOR_ROLE_NAME);
        subsystemUser.setRoles(List.of(editorRole));
        return subsystemUser;
    }

    @NotNull
    private static SubsystemRole createSupersetRole(int id, String roleName) {
        SubsystemRole role = new SubsystemRole();
        role.setId(id);
        role.setName(roleName);
        return role;
    }

    @NotNull
    private DashboardResource createDashboardResource() {
        return createDashboardResource(false);
    }

    @NotNull
    private DashboardResource createDashboardResource(boolean withAdditionalViewerRole) {
        List<SupersetDashboard> supersetDashboards = new ArrayList<>();
        SupersetDashboard dashboard = new SupersetDashboard();
        dashboard.setId(1);
        dashboard.setDashboardTitle("Dashboard " + 1);
        dashboard.setPublished(true);
        dashboard.setRoles(List.of(createSupersetRole(1, SlugifyUtil.slugify("dashboard-1"))));
        supersetDashboards.add(dashboard);
        if (withAdditionalViewerRole) {
            SupersetDashboard dashboard2 = new SupersetDashboard();
            dashboard2.setId(2);
            dashboard2.setDashboardTitle("Dashboard " + 2);
            dashboard2.setPublished(true);
            dashboard2.setRoles(List.of(createSupersetRole(2, SlugifyUtil.slugify("dashboard-2"))));
            supersetDashboards.add(dashboard2);
        }
        return new DashboardResource(instanceName, "", supersetDashboards);
    }

    private List<MetaInfoResourceEntity> createMetaInfoResourceEntities(DashboardResource dashboardResource) {
        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        metaInfoResourceEntity.setMetainfo(dashboardResource);
        metaInfoResourceEntity.setContextKey(instanceName);
        return List.of(metaInfoResourceEntity);
    }
}
