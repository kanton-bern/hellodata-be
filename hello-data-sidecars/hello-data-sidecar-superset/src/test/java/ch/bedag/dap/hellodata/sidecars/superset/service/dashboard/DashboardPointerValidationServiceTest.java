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
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardPointerValidationServiceTest {

    @Mock
    private SupersetClient supersetClient;

    @InjectMocks
    private DashboardPointerValidationService service;

    private static JsonObject json(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    @Test
    void blankPointerIsValid() {
        assertThat(service.isPointerValid(supersetClient, "")).isTrue();
        assertThat(service.isPointerValid(supersetClient, null)).isTrue();
    }

    @Test
    void dashboardPermalinkWithMissingKeyIsInvalid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc")).thenReturn(null);

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isFalse();
    }

    @Test
    void dashboardPermalinkWithMissingAnchorComponentIsInvalid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc"))
                .thenReturn(json("{\"dashboardId\":\"10\",\"state\":{\"anchor\":\"CHART-deleted\"}}"));
        when(supersetClient.getDashboardComponentIds("10"))
                .thenReturn(Optional.of(Set.of("CHART-kept", "GRID_ID")));

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isFalse();
    }

    @Test
    void dashboardPermalinkWithPresentAnchorComponentIsValid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc"))
                .thenReturn(json("{\"dashboardId\":\"10\",\"state\":{\"anchor\":\"CHART-kept\"}}"));
        when(supersetClient.getDashboardComponentIds("10"))
                .thenReturn(Optional.of(Set.of("CHART-kept", "GRID_ID")));

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isTrue();
    }

    @Test
    void dashboardPermalinkWithoutAnchorIsValid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc"))
                .thenReturn(json("{\"dashboardId\":\"10\",\"state\":{}}"));

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isTrue();
    }

    @Test
    void dashboardPermalinkWithChartAnchorStillInDashboardIsValid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc"))
                .thenReturn(json("{\"dashboardId\":\"10\",\"state\":{\"anchor\":\"CHART-explore-156-1\"}}"));
        when(supersetClient.getDashboardChartIds("10"))
                .thenReturn(Optional.of(Set.of(153, 154, 156)));

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isTrue();
    }

    @Test
    void dashboardPermalinkWithChartAnchorRemovedFromDashboardIsInvalid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc"))
                .thenReturn(json("{\"dashboardId\":\"10\",\"state\":{\"anchor\":\"CHART-explore-156-1\"}}"));
        when(supersetClient.getDashboardChartIds("10"))
                .thenReturn(Optional.of(Set.of(153, 154)));

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isFalse();
    }

    @Test
    void dashboardPermalinkWithChartAnchorAndUnreadableLayoutIsValid() throws Exception {
        when(supersetClient.getDashboardPermalinkState("abc"))
                .thenReturn(json("{\"dashboardId\":\"10\",\"state\":{\"anchor\":\"CHART-explore-156-1\"}}"));
        when(supersetClient.getDashboardChartIds("10")).thenReturn(Optional.empty());

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isTrue();
    }

    @Test
    void explorePermalinkWithDeletedChartIsInvalid() throws Exception {
        when(supersetClient.getExplorePermalinkState("xyz"))
                .thenReturn(json("{\"chartId\":42}"));
        when(supersetClient.chartExists(42)).thenReturn(false);

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/explore/p/xyz/")).isFalse();
    }

    @Test
    void explorePermalinkWithExistingChartIsValid() throws Exception {
        when(supersetClient.getExplorePermalinkState("xyz"))
                .thenReturn(json("{\"chartId\":42}"));
        when(supersetClient.chartExists(42)).thenReturn(true);

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/explore/p/xyz/")).isTrue();
    }

    @Test
    void sliceIdParamWithDeletedChartIsInvalid() throws Exception {
        when(supersetClient.chartExists(7)).thenReturn(false);

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/explore/?slice_id=7")).isFalse();
    }

    @Test
    void unknownUrlShapeIsValid() {
        assertThat(service.isPointerValid(supersetClient, "https://superset/some/other/page")).isTrue();
    }

    @Test
    void lookupFailureIsTreatedAsValid() throws Exception {
        lenient().when(supersetClient.getDashboardPermalinkState("abc")).thenThrow(new RuntimeException("boom"));

        assertThat(service.isPointerValid(supersetClient, "https://superset/superset/dashboard/p/abc/")).isTrue();
    }
}
