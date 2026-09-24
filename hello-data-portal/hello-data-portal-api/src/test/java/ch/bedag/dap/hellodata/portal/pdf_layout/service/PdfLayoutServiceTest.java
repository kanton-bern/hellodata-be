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
package ch.bedag.dap.hellodata.portal.pdf_layout.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.DashboardPaletteResponse;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSaveDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSummaryDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.entity.PdfLayoutEntity;
import ch.bedag.dap.hellodata.portal.pdf_layout.entity.PdfLayoutItem;
import ch.bedag.dap.hellodata.portal.pdf_layout.repository.PdfLayoutRepository;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.PaletteClient;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfLayoutServiceTest {

    private static final String INSTANCE = "superset-ctx1";
    private static final long DASHBOARD_ID = 7;
    private static final UUID USER_ID = UUID.randomUUID();

    @InjectMocks
    private PdfLayoutService pdfLayoutService;

    @Mock
    private PdfLayoutRepository pdfLayoutRepository;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private PaletteClient paletteClient;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void createLayout_persistsCompleteLayoutForCurrentUser() {
        // given
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of(dashboard()));
        when(pdfLayoutRepository.save(any(PdfLayoutEntity.class))).thenAnswer(inv -> {
            PdfLayoutEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // when
        PdfLayoutDto result = pdfLayoutService.createLayout(saveDto("  Monthly  "));

        // then
        ArgumentCaptor<PdfLayoutEntity> captor = ArgumentCaptor.forClass(PdfLayoutEntity.class);
        verify(pdfLayoutRepository).save(captor.capture());
        PdfLayoutEntity saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("Monthly", saved.getName());
        assertEquals("ctx1", saved.getContextKey());
        assertEquals("Sales", saved.getDashboardTitle());
        assertEquals("landscape", saved.getTemplate());
        assertEquals(2, saved.getPageCount());
        assertEquals(4, saved.getGridCols());
        assertEquals(4, saved.getGridRows());
        assertEquals(2, saved.getItems().size());
        assertNotNull(result.getId());
        assertEquals(2, result.getItems().size());
    }

    @Test
    void createLayout_rejectsDuplicateName() {
        // given
        PdfLayoutEntity existing = entity(List.of());
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of(dashboard()));
        when(pdfLayoutRepository.findByUserIdAndInstanceNameAndDashboardIdAndNameIgnoreCase(USER_ID, INSTANCE, DASHBOARD_ID, "Monthly"))
                .thenReturn(Optional.of(existing));

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.createLayout(saveDto("Monthly")));

        // then
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(pdfLayoutRepository, never()).save(any());
    }

    @Test
    void createLayout_rejectsInaccessibleDashboard() {
        // given
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of());

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.createLayout(saveDto("Monthly")));

        // then
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(pdfLayoutRepository, never()).save(any());
    }

    @Test
    void createLayout_reportsAllValidationErrors() {
        // given
        PdfLayoutSaveDto dto = saveDto(" ");
        dto.setTemplate("a3");
        dto.setPageCount(1);
        dto.getItems().add(PdfLayoutItem.builder().type("chart").page(0).x(3).y(0).cols(2).rows(1).build());

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.createLayout(dto));

        // then
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        String reason = ex.getReason();
        assertNotNull(reason);
        assertTrue(reason.contains("Name is required"));
        assertTrue(reason.contains("Unknown template 'a3'"));
        assertTrue(reason.contains("Cell 2: page is out of range"));
        assertTrue(reason.contains("Cell 3: chart id is required"));
        assertTrue(reason.contains("Cell 3: position is outside the page grid"));
        verifyNoInteractions(dashboardService);
    }

    @Test
    void createLayout_withoutPortalUser_isForbidden() {
        // given
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(null);

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.createLayout(saveDto("Monthly")));

        // then
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateLayout_updatesExistingInsteadOfCreatingDuplicate() {
        // given
        PdfLayoutEntity existing = entity(List.of());
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));
        when(pdfLayoutRepository.findByUserIdAndInstanceNameAndDashboardIdAndNameIgnoreCase(USER_ID, INSTANCE, DASHBOARD_ID, "Monthly"))
                .thenReturn(Optional.of(existing));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of(dashboard()));
        when(pdfLayoutRepository.save(existing)).thenReturn(existing);

        // when
        PdfLayoutDto result = pdfLayoutService.updateLayout(existing.getId(), saveDto("Monthly"));

        // then
        assertEquals(existing.getId(), result.getId());
        assertEquals("landscape", existing.getTemplate());
        assertEquals(2, existing.getItems().size());
        verify(pdfLayoutRepository).save(existing);
    }

    @Test
    void updateLayout_ofAnotherUser_isNotFound() {
        // given
        UUID id = UUID.randomUUID();
        when(pdfLayoutRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.updateLayout(id, saveDto("Monthly")));

        // then
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(pdfLayoutRepository, never()).save(any());
    }

    @Test
    void loadLayout_dropsChartsThatNoLongerExist() {
        // given
        PdfLayoutEntity existing = entity(List.of(
                chart(1L, "Revenue"),
                chart(2L, "Costs"),
                chart(3L, "Margin"),
                PdfLayoutItem.builder().type("markdown").html("<p>Hi</p>").page(0).x(0).y(3).cols(4).rows(1).build()));
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of(dashboard()));
        when(paletteClient.fetchPalette(INSTANCE, DASHBOARD_ID))
                .thenReturn(new DashboardPaletteResponse(List.of(new DashboardPaletteResponse.ChartRef(1L, "Revenue")), "{}"));

        // when
        PdfLayoutDto result = pdfLayoutService.loadLayout(existing.getId());

        // then
        assertEquals(List.of("Costs", "Margin"), result.getRemovedCharts());
        assertEquals(2, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getChartId());
        assertEquals("markdown", result.getItems().get(1).getType());
    }

    @Test
    void loadLayout_failsWhenDashboardNoLongerExists() {
        // given
        PdfLayoutEntity existing = entity(List.of(chart(1L, "Revenue")));
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of());

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.loadLayout(existing.getId()));

        // then
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Dashboard Sales not found.", ex.getReason());
        verifyNoInteractions(paletteClient);
    }

    @Test
    void loadLayout_ofInaccessibleDashboard_isForbidden() {
        // given
        PdfLayoutEntity existing = entity(List.of(chart(1L, "Revenue")));
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of());
        dashboardStillExists(DASHBOARD_ID);

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.loadLayout(existing.getId()));

        // then
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(paletteClient);
    }

    @Test
    void updateLayout_ofInaccessibleDashboard_isForbidden() {
        // given
        PdfLayoutEntity existing = entity(List.of());
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of());
        dashboardStillExists(DASHBOARD_ID);

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.updateLayout(existing.getId(), saveDto("Monthly")));

        // then
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(pdfLayoutRepository, never()).save(any());
    }

    @Test
    void deleteLayout_ofInaccessibleDashboard_isForbidden() {
        // given
        PdfLayoutEntity existing = entity(List.of());
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of());
        dashboardStillExists(DASHBOARD_ID);

        // when
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> pdfLayoutService.deleteLayout(existing.getId()));

        // then
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(pdfLayoutRepository, never()).delete(any());
    }

    @Test
    void findMyLayouts_hidesLayoutsOfInaccessibleDashboards() {
        // given
        PdfLayoutEntity accessible = entity(List.of());
        PdfLayoutEntity inaccessible = entity(List.of());
        inaccessible.setName("Hidden");
        inaccessible.setDashboardId(8);
        PdfLayoutEntity gone = entity(List.of());
        gone.setName("Gone");
        gone.setDashboardId(9);
        when(pdfLayoutRepository.findAllByUserIdOrderByNameAsc(USER_ID)).thenReturn(List.of(accessible, inaccessible, gone));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of(dashboard()));
        dashboardStillExists(DASHBOARD_ID, 8);

        // when
        List<PdfLayoutSummaryDto> result = pdfLayoutService.findMyLayouts(null);

        // then
        assertEquals(List.of("Monthly", "Gone"), result.stream().map(PdfLayoutSummaryDto::getName).toList());
    }

    @Test
    void findMyLayouts_filtersByContextKeyWhenGiven() {
        // given
        when(pdfLayoutRepository.findAllByUserIdAndContextKeyOrderByNameAsc(USER_ID, "ctx1")).thenReturn(List.of(entity(List.of())));
        when(dashboardService.fetchMyDashboards()).thenReturn(Set.of(dashboard()));

        // when
        List<PdfLayoutSummaryDto> result = pdfLayoutService.findMyLayouts("ctx1");

        // then
        assertEquals(1, result.size());
        assertEquals("Monthly", result.get(0).getName());
        verify(pdfLayoutRepository, never()).findAllByUserIdOrderByNameAsc(any());
    }

    @Test
    void deleteLayout_deletesOwnedLayout() {
        // given
        PdfLayoutEntity existing = entity(List.of());
        when(pdfLayoutRepository.findByIdAndUserId(existing.getId(), USER_ID)).thenReturn(Optional.of(existing));

        // when
        pdfLayoutService.deleteLayout(existing.getId());

        // then
        verify(pdfLayoutRepository).delete(existing);
    }

    private void dashboardStillExists(long... dashboardIds) {
        List<SupersetDashboard> dashboards = new ArrayList<>();
        for (long dashboardId : dashboardIds) {
            SupersetDashboard d = new SupersetDashboard();
            d.setId((int) dashboardId);
            dashboards.add(d);
        }
        when(metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, INSTANCE, ModuleResourceKind.HELLO_DATA_DASHBOARDS, DashboardResource.class))
                .thenReturn(new DashboardResource(INSTANCE, dashboards));
    }

    private static SupersetDashboardDto dashboard() {
        SupersetDashboardDto d = new SupersetDashboardDto();
        d.setId((int) DASHBOARD_ID);
        d.setInstanceName(INSTANCE);
        d.setDashboardTitle("Sales");
        d.setContextKey("ctx1");
        return d;
    }

    private static PdfLayoutItem chart(Long chartId, String name) {
        return PdfLayoutItem.builder().type("chart").chartId(chartId).name(name).page(0).x(0).y(0).cols(2).rows(2).build();
    }

    private static PdfLayoutSaveDto saveDto(String name) {
        PdfLayoutSaveDto dto = new PdfLayoutSaveDto();
        dto.setName(name);
        dto.setInstanceName(INSTANCE);
        dto.setDashboardId(DASHBOARD_ID);
        dto.setTemplate("landscape");
        dto.setPageCount(2);
        dto.setItems(new ArrayList<>(List.of(
                chart(1L, "Revenue"),
                PdfLayoutItem.builder().type("markdown").html("<p>Notes</p>").readonly(false).page(1).x(0).y(0).cols(4).rows(1).build())));
        return dto;
    }

    private static PdfLayoutEntity entity(List<PdfLayoutItem> items) {
        PdfLayoutEntity e = new PdfLayoutEntity();
        e.setId(UUID.randomUUID());
        e.setUserId(USER_ID);
        e.setName("Monthly");
        e.setContextKey("ctx1");
        e.setInstanceName(INSTANCE);
        e.setDashboardId(DASHBOARD_ID);
        e.setDashboardTitle("Sales");
        e.setTemplate("portrait");
        e.setPageCount(1);
        e.setGridCols(4);
        e.setGridRows(4);
        e.setItems(new ArrayList<>(items));
        return e;
    }
}
