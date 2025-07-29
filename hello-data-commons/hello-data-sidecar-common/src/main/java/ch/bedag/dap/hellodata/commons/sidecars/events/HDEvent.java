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
package ch.bedag.dap.hellodata.commons.sidecars.events;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.PermissionResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.storage.data.StorageMonitoringResult;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.*;
import lombok.Getter;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDStream.METAINFO_STREAM;

@Getter
public enum HDEvent {
    PUBLISH_APP_INFO_RESOURCES(METAINFO_STREAM, "publish_resources.app_info", AppInfoResource.class),
    PUBLISH_DASHBOARD_RESOURCES(METAINFO_STREAM, "publish_resources.dashboard", DashboardResource.class),
    PUBLISH_ROLE_RESOURCES(METAINFO_STREAM, "publish_resources.role", RoleResource.class),
    PUBLISH_PERMISSION_RESOURCES(METAINFO_STREAM, "publish_resources.permission", PermissionResource.class),
    PUBLISH_DATABASE_RESOURCES(METAINFO_STREAM, "publish_resources.database", PermissionResource.class),
    PUBLISH_PIPELINE_RESOURCES(METAINFO_STREAM, "publish_resources.pipeline", PipelineResource.class),
    PUBLISH_USER_RESOURCES(METAINFO_STREAM, "publish_resources.user", UserResource.class),
    UPDATE_METAINFO_USERS_CACHE(METAINFO_STREAM, "user_cache_update", UserCacheUpdate.class),
    DISABLE_USER(METAINFO_STREAM, "disable_user", SubsystemUserUpdate.class),
    ENABLE_USER(METAINFO_STREAM, "enable_user", SubsystemUserUpdate.class),
    CREATE_USER(METAINFO_STREAM, "create_user", SubsystemUserUpdate.class),
    UPDATE_USER_CONTEXT_ROLE(METAINFO_STREAM, "update_user_context_role", UserContextRoleUpdate.class),
    COMMENCE_USERS_SYNC(METAINFO_STREAM, "commence_users_sync"),
    SYNC_USERS(METAINFO_STREAM, "sync_users", AllUsersContextRoleUpdate.class),
    UPDATE_STORAGE_MONITORING_RESULT(METAINFO_STREAM, "update_storage_monitoring_result", StorageMonitoringResult.class),
    DELETE_USER(METAINFO_STREAM, "delete_user", SubsystemUserDelete.class),
    GET_ALL_USERS(METAINFO_STREAM, "users_refresh", SubsystemGetAllUsers.class),
    ;
    private final HDStream stream;
    private final String subject;
    private final Class<?> dataClass;

    HDEvent(HDStream stream, String subject, Class<?> dataClass) {
        this.stream = stream;
        this.subject = subject;
        this.dataClass = dataClass;
    }

    HDEvent(HDStream stream, String subject) {
        this.stream = stream;
        this.subject = subject;
        this.dataClass = Void.class;
    }

    public String getStreamName() {
        return stream.name();
    }
}
