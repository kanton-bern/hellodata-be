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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.DashboardPaletteResponse;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSaveDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSummaryDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.entity.PdfLayoutEntity;
import ch.bedag.dap.hellodata.portal.pdf_layout.entity.PdfLayoutItem;
import ch.bedag.dap.hellodata.portal.pdf_layout.repository.PdfLayoutRepository;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.PaletteClient;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.ReportTemplate;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Saved custom PDF layouts. There are no dedicated permissions for this area yet, so a layout is
 * private to the user who saved it: every read and write is scoped to the current user's id, and a
 * layout of another user is reported as not found.
 * <p>
 * A layout is also strictly bound to its data domain dashboard: whoever may not see the dashboard
 * (any more) may not see or touch the layout either. Such layouts are hidden from the list and
 * refused with 403. Only a layout whose dashboard no longer exists at all stays listed, so the user
 * gets "Dashboard X not found." when loading it and can still delete it.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PdfLayoutService {

    static final int MAX_NAME_LENGTH = 255;
    static final int MAX_PAGES = 50;
    static final int MAX_TEXT_LENGTH = 100_000;

    private final PdfLayoutRepository pdfLayoutRepository;
    private final DashboardService dashboardService;
    private final PaletteClient paletteClient;
    private final MetaInfoResourceService metaInfoResourceService;

    /** The current user's layouts, optionally narrowed to one data domain. */
    @Transactional(readOnly = true)
    public List<PdfLayoutSummaryDto> findMyLayouts(String contextKey) {
        UUID userId = currentUserId();
        List<PdfLayoutEntity> layouts = StringUtils.isBlank(contextKey)
                ? pdfLayoutRepository.findAllByUserIdOrderByNameAsc(userId)
                : pdfLayoutRepository.findAllByUserIdAndContextKeyOrderByNameAsc(userId, contextKey);
        if (layouts.isEmpty()) {
            return List.of();
        }
        Set<SupersetDashboardDto> myDashboards = dashboardService.fetchMyDashboards();
        return layouts.stream()
                .filter(l -> findDashboard(myDashboards, l.getInstanceName(), l.getDashboardId()).isPresent()
                        || !dashboardExists(l.getInstanceName(), l.getDashboardId()))
                .map(this::toSummary)
                .toList();
    }

    /**
     * Loads a layout for the builder. Fails with "Dashboard X not found." when its dashboard no longer
     * exists (or is no longer accessible); charts that were removed from the dashboard are dropped
     * from the returned cells and named in {@link PdfLayoutDto#getRemovedCharts()}.
     */
    @Transactional(readOnly = true)
    public PdfLayoutDto loadLayout(UUID id) {
        PdfLayoutEntity entity = findOwned(id);
        SupersetDashboardDto dashboard = requireDashboardAccess(entity);

        Set<Long> existingChartIds = Optional.ofNullable(paletteClient.fetchPalette(entity.getInstanceName(), entity.getDashboardId()).getCharts())
                .orElse(List.of()).stream()
                .map(DashboardPaletteResponse.ChartRef::getId)
                .collect(Collectors.toSet());

        List<PdfLayoutItem> kept = new ArrayList<>();
        Set<String> removed = new LinkedHashSet<>();
        for (PdfLayoutItem item : Optional.ofNullable(entity.getItems()).orElse(List.of())) {
            if (PdfLayoutItem.TYPE_CHART.equals(item.getType()) && !existingChartIds.contains(item.getChartId())) {
                removed.add(StringUtils.defaultIfBlank(item.getName(), "#" + item.getChartId()));
            } else {
                kept.add(item);
            }
        }
        if (!removed.isEmpty()) {
            log.info("PDF layout {} loaded without removed charts {}", id, removed);
        }

        PdfLayoutDto dto = toDto(entity);
        dto.setDashboardTitle(dashboard.getDashboardTitle());
        dto.setItems(kept);
        dto.setRemovedCharts(new ArrayList<>(removed));
        return dto;
    }

    @Transactional
    public PdfLayoutDto createLayout(PdfLayoutSaveDto saveDto) {
        UUID userId = currentUserId();
        validate(saveDto);
        SupersetDashboardDto dashboard = requireAccessibleDashboard(saveDto);
        assertNameUnique(userId, saveDto, null);
        PdfLayoutEntity entity = new PdfLayoutEntity();
        entity.setUserId(userId);
        apply(entity, saveDto, dashboard);
        return toDto(pdfLayoutRepository.save(entity));
    }

    @Transactional
    public PdfLayoutDto updateLayout(UUID id, PdfLayoutSaveDto saveDto) {
        PdfLayoutEntity entity = findOwned(id);
        requireDashboardAccess(entity);
        validate(saveDto);
        SupersetDashboardDto dashboard = requireAccessibleDashboard(saveDto);
        assertNameUnique(entity.getUserId(), saveDto, id);
        apply(entity, saveDto, dashboard);
        return toDto(pdfLayoutRepository.save(entity));
    }

    @Transactional
    public void deleteLayout(UUID id) {
        PdfLayoutEntity entity = findOwned(id);
        // A layout of a deleted dashboard may still be cleaned up; one of an inaccessible dashboard not.
        if (findDashboard(entity.getInstanceName(), entity.getDashboardId()).isEmpty()) {
            assertDashboardGone(entity);
        }
        pdfLayoutRepository.delete(entity);
    }

    private UUID currentUserId() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not known to the portal");
        }
        return userId;
    }

    private PdfLayoutEntity findOwned(UUID id) {
        return pdfLayoutRepository.findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF layout not found."));
    }

    /** The layout's dashboard if the current user may access it; 403 when it exists but is not
     *  accessible, 404 "Dashboard X not found." when it no longer exists. */
    private SupersetDashboardDto requireDashboardAccess(PdfLayoutEntity entity) {
        Optional<SupersetDashboardDto> dashboard = findDashboard(entity.getInstanceName(), entity.getDashboardId());
        if (dashboard.isPresent()) {
            return dashboard.get();
        }
        assertDashboardGone(entity);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard " + dashboardLabel(entity) + " not found.");
    }

    private void assertDashboardGone(PdfLayoutEntity entity) {
        if (dashboardExists(entity.getInstanceName(), entity.getDashboardId())) {
            log.warn("User {} attempted to use PDF layout {} of dashboard {}/{} without access", SecurityUtils.getCurrentUserEmail(), entity.getId(),
                    entity.getInstanceName(), entity.getDashboardId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to dashboard " + dashboardLabel(entity) + ".");
        }
    }

    private static String dashboardLabel(PdfLayoutEntity entity) {
        return StringUtils.defaultIfBlank(entity.getDashboardTitle(), String.valueOf(entity.getDashboardId()));
    }

    /** True when the dashboard still exists in its Superset instance, regardless of the user's access. */
    private boolean dashboardExists(String instanceName, long dashboardId) {
        DashboardResource resource = metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName,
                ModuleResourceKind.HELLO_DATA_DASHBOARDS, DashboardResource.class);
        return resource != null && CollectionUtils.emptyIfNull(resource.getData()).stream().anyMatch(d -> d.getId() == dashboardId);
    }

    private Optional<SupersetDashboardDto> findDashboard(String instanceName, long dashboardId) {
        return findDashboard(dashboardService.fetchMyDashboards(), instanceName, dashboardId);
    }

    private static Optional<SupersetDashboardDto> findDashboard(Set<SupersetDashboardDto> dashboards, String instanceName, long dashboardId) {
        return dashboards.stream()
                .filter(d -> d.getInstanceName() != null && d.getInstanceName().equalsIgnoreCase(instanceName) && d.getId() == dashboardId)
                .findFirst();
    }

    private SupersetDashboardDto requireAccessibleDashboard(PdfLayoutSaveDto saveDto) {
        return findDashboard(saveDto.getInstanceName(), saveDto.getDashboardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard " + saveDto.getDashboardId() + " not found."));
    }

    /** One user can't have two layouts of the same name on the same dashboard; saving under an
     *  existing name must go through an update of that layout instead. */
    private void assertNameUnique(UUID userId, PdfLayoutSaveDto saveDto, UUID selfId) {
        pdfLayoutRepository.findByUserIdAndInstanceNameAndDashboardIdAndNameIgnoreCase(userId, saveDto.getInstanceName(), saveDto.getDashboardId(), saveDto.getName().trim())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A layout named '" + saveDto.getName().trim() + "' already exists for this dashboard.");
                });
    }

    private void apply(PdfLayoutEntity entity, PdfLayoutSaveDto saveDto, SupersetDashboardDto dashboard) {
        entity.setName(saveDto.getName().trim());
        entity.setContextKey(dashboard.getContextKey());
        entity.setInstanceName(dashboard.getInstanceName());
        entity.setDashboardId(saveDto.getDashboardId());
        entity.setDashboardTitle(dashboard.getDashboardTitle());
        entity.setTemplate(saveDto.getTemplate());
        entity.setPageCount(saveDto.getPageCount());
        entity.setGridCols(ReportTemplate.GRID_COLS);
        entity.setGridRows(ReportTemplate.GRID_ROWS_PER_PAGE);
        entity.setItems(new ArrayList<>(saveDto.getItems()));
    }

    /** Checks the whole layout and reports every violation at once, so the user sees all of them. */
    void validate(PdfLayoutSaveDto saveDto) {
        List<String> errors = new ArrayList<>();
        if (StringUtils.isBlank(saveDto.getName())) {
            errors.add("Name is required");
        } else if (saveDto.getName().trim().length() > MAX_NAME_LENGTH) {
            errors.add("Name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        if (StringUtils.isBlank(saveDto.getInstanceName()) || saveDto.getDashboardId() <= 0) {
            errors.add("Dashboard is required");
        }
        if (Arrays.stream(ReportTemplate.values()).noneMatch(t -> t.id().equals(saveDto.getTemplate()))) {
            errors.add("Unknown template '" + saveDto.getTemplate() + "'");
        }
        if (saveDto.getPageCount() < 1 || saveDto.getPageCount() > MAX_PAGES) {
            errors.add("Page count must be between 1 and " + MAX_PAGES);
        }
        if (saveDto.getItems() == null) {
            errors.add("Items are required");
        } else {
            for (int i = 0; i < saveDto.getItems().size(); i++) {
                validateItem(saveDto.getItems().get(i), i + 1, saveDto.getPageCount(), errors);
            }
        }
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid layout: " + String.join("; ", errors));
        }
    }

    private void validateItem(PdfLayoutItem item, int index, int pageCount, List<String> errors) {
        String prefix = "Cell " + index + ": ";
        if (item == null) {
            errors.add(prefix + "is empty");
            return;
        }
        if (PdfLayoutItem.TYPE_CHART.equals(item.getType())) {
            if (item.getChartId() == null) {
                errors.add(prefix + "chart id is required");
            }
        } else if (!PdfLayoutItem.TYPE_MARKDOWN.equals(item.getType())) {
            errors.add(prefix + "unknown type '" + item.getType() + "'");
        }
        if (StringUtils.length(item.getHtml()) > MAX_TEXT_LENGTH || StringUtils.length(item.getMarkdown()) > MAX_TEXT_LENGTH) {
            errors.add(prefix + "text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (item.getPage() < 0 || item.getPage() >= pageCount) {
            errors.add(prefix + "page is out of range");
        }
        if (item.getCols() < 1 || item.getRows() < 1 || item.getX() < 0 || item.getY() < 0
                || item.getX() + item.getCols() > ReportTemplate.GRID_COLS
                || item.getY() + item.getRows() > ReportTemplate.GRID_ROWS_PER_PAGE) {
            errors.add(prefix + "position is outside the page grid");
        }
    }

    private PdfLayoutSummaryDto toSummary(PdfLayoutEntity entity) {
        PdfLayoutSummaryDto dto = new PdfLayoutSummaryDto();
        fillSummary(dto, entity);
        return dto;
    }

    private PdfLayoutDto toDto(PdfLayoutEntity entity) {
        PdfLayoutDto dto = new PdfLayoutDto();
        fillSummary(dto, entity);
        dto.setPageCount(entity.getPageCount());
        dto.setGridCols(entity.getGridCols());
        dto.setGridRows(entity.getGridRows());
        dto.setItems(new ArrayList<>(Optional.ofNullable(entity.getItems()).orElse(List.of())));
        return dto;
    }

    private void fillSummary(PdfLayoutSummaryDto dto, PdfLayoutEntity entity) {
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setContextKey(entity.getContextKey());
        dto.setInstanceName(entity.getInstanceName());
        dto.setDashboardId(entity.getDashboardId());
        dto.setDashboardTitle(entity.getDashboardTitle());
        dto.setTemplate(entity.getTemplate());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setModifiedDate(entity.getModifiedDate());
    }
}
