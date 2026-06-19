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
package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves whether a dashboard-comment pointer URL (a Superset permalink, chart or dashboard link)
 * still points to an existing target in Superset.
 *
 * <p>The check is deliberately conservative: if the type of a link cannot be determined or a lookup
 * fails for a reason other than "not found", the pointer is reported as valid so that working links
 * are never greyed out by mistake.</p>
 */
@Log4j2
@Service
public class DashboardPointerValidationService {

    private static final Pattern DASHBOARD_PERMALINK = Pattern.compile("/dashboard/p/([^/?#]+)");
    private static final Pattern EXPLORE_PERMALINK = Pattern.compile("/explore/p/([^/?#]+)");
    private static final Pattern CHART_PATH = Pattern.compile("/chart/(\\d+)");
    private static final Pattern DASHBOARD_PATH = Pattern.compile("/dashboard/([^/?#]+)");
    private static final Pattern SLICE_ID_PARAM = Pattern.compile("[?&]slice_id=(\\d+)");
    // A permalink chart anchor has the shape "CHART-explore-<sliceId>-<n>" and references the chart by id.
    private static final Pattern ANCHOR_CHART_ID = Pattern.compile("CHART-explore-(\\d+)");

    public boolean isPointerValid(SupersetClient supersetClient, String pointerUrl) {
        if (pointerUrl == null || pointerUrl.isBlank()) {
            return true;
        }
        try {
            Matcher dashboardPermalink = DASHBOARD_PERMALINK.matcher(pointerUrl);
            if (dashboardPermalink.find()) {
                return isDashboardPermalinkValid(supersetClient, dashboardPermalink.group(1));
            }

            Matcher explorePermalink = EXPLORE_PERMALINK.matcher(pointerUrl);
            if (explorePermalink.find()) {
                return isExplorePermalinkValid(supersetClient, explorePermalink.group(1));
            }

            Matcher sliceId = SLICE_ID_PARAM.matcher(pointerUrl);
            if (sliceId.find()) {
                return supersetClient.chartExists(Integer.parseInt(sliceId.group(1)));
            }

            Matcher chartPath = CHART_PATH.matcher(pointerUrl);
            if (chartPath.find()) {
                return supersetClient.chartExists(Integer.parseInt(chartPath.group(1)));
            }

            Matcher dashboardPath = DASHBOARD_PATH.matcher(pointerUrl);
            if (dashboardPath.find()) {
                return supersetClient.dashboardExists(dashboardPath.group(1));
            }

            // Unknown link shape - do not flag it as broken.
            return true;
        } catch (Exception e) {
            log.warn("Could not validate dashboard pointer '{}'. Treating it as valid.", pointerUrl, e);
            return true;
        }
    }

    private boolean isDashboardPermalinkValid(SupersetClient supersetClient, String key) throws Exception {
        JsonObject permalink = supersetClient.getDashboardPermalinkState(key);
        if (permalink == null) {
            // permalink key or its referenced dashboard no longer exists
            return false;
        }
        String anchor = readAnchor(permalink);
        if (anchor == null || anchor.isBlank()) {
            // points to the dashboard as a whole, which still exists
            return true;
        }
        String dashboardId = permalink.has("dashboardId") && !permalink.get("dashboardId").isJsonNull()
                ? permalink.get("dashboardId").getAsString() : null;
        if (dashboardId == null) {
            return true;
        }

        // A chart anchor ("CHART-explore-<sliceId>-<n>") references a chart by id; it is only valid
        // while that chart is still part of the dashboard layout.
        Matcher anchorChartId = ANCHOR_CHART_ID.matcher(anchor);
        if (anchorChartId.find()) {
            int sliceId = Integer.parseInt(anchorChartId.group(1));
            Optional<Set<Integer>> chartIds = supersetClient.getDashboardChartIds(dashboardId);
            if (chartIds.isEmpty()) {
                // could not read the layout - avoid a false negative
                return true;
            }
            return chartIds.get().contains(sliceId);
        }

        // Otherwise the anchor is a layout component id; match it against the dashboard's components.
        Optional<Set<String>> componentIds = supersetClient.getDashboardComponentIds(dashboardId);
        if (componentIds.isEmpty()) {
            // could not read the layout - avoid a false negative
            return true;
        }
        return componentIds.get().contains(anchor);
    }

    private boolean isExplorePermalinkValid(SupersetClient supersetClient, String key) throws Exception {
        JsonObject permalink = supersetClient.getExplorePermalinkState(key);
        if (permalink == null) {
            return false;
        }
        if (permalink.has("chartId") && !permalink.get("chartId").isJsonNull()) {
            return supersetClient.chartExists(permalink.get("chartId").getAsInt());
        }
        return true;
    }

    private String readAnchor(JsonObject permalink) {
        if (permalink.has("state") && permalink.get("state").isJsonObject()) {
            JsonObject state = permalink.getAsJsonObject("state");
            if (state.has("anchor") && !state.get("anchor").isJsonNull()) {
                return state.get("anchor").getAsString();
            }
        }
        return null;
    }
}
