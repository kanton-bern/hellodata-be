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
package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupersetDashboard implements Serializable {
    private int id;
    @JsonProperty("dashboard_title")
    private String dashboardTitle;
    @JsonProperty("certification_details")
    private String certificationDetails;
    @JsonProperty("certified_by")
    private String certifiedBy;
    @JsonProperty("changed_by")
    private SubsystemUser changedBy;
    @JsonProperty("changed_by_name")
    private String changedByName;
    @JsonProperty("changed_by_url")
    private String changedByUrl;
    @JsonProperty("changed_on_delta_humanized")
    private String changedOnDeltaHumanized;
    @JsonProperty("changed_on_utc")
    private String changedOnUtc;
    @JsonProperty("created_by")
    private SubsystemUser createdBy;
    @JsonProperty("created_on_delta_humanized")
    private String createdOnDeltaHumanized;
    @JsonProperty("is_managed_externally")
    private boolean isManagedExternally;
    private List<SubsystemUser> owners;
    private boolean published;

    private List<SubsystemRole> roles;
    private String slug;
    private String status;
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    private String url;
}
