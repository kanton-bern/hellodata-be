--
-- Copyright © 2024, Kanton Bern
-- All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are met:
--     * Redistributions of source code must retain the above copyright
--       notice, this list of conditions and the following disclaimer.
--     * Redistributions in binary form must reproduce the above copyright
--       notice, this list of conditions and the following disclaimer in the
--       documentation and/or other materials provided with the distribution.
--     * Neither the name of the <organization> nor the
--       names of its contributors may be used to endorse or promote products
--       derived from this software without specific prior written permission.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
-- ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
-- WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
-- DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
-- DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
-- (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
-- LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
-- (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
-- SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--

--
-- Sets permissions for HD roles.
-- For BI_ADMIN, BI_EDITOR and B_VIEWER role.
-- Is repeatable.
--


--
-- Drop temporary table temp_permission_view_role
--
DROP TABLE IF EXISTS temp_permission_view_role;


--
-- Create temporary table temp_permission_view_role if not exists
--
CREATE TEMPORARY TABLE temp_permission_view_role (
    permission_name VARCHAR(100),
    view_name VARCHAR(255),
    role_name VARCHAR(64),
    CONSTRAINT temp_permission_view_role_unique_row UNIQUE (permission_name, view_name, role_name)
);


--
-- Add default permissions for BI_ADMIN_TEMP role in temp_permission_view_role
--
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','Database','BI_ADMIN_TEMP');

--
-- Add default permissions for BI_EDITOR_TEMP role in temp_permission_view_role
--
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Access requests','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_add','AccessRequestsModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','AccessRequestsModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_edit','AccessRequestsModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list','AccessRequestsModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_show','AccessRequestsModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('muldelete','AccessRequestsModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','AdvancedDataType','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Alerts & Report','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('all_database_access','all_database_access','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('all_datasource_access','all_datasource_access','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Annotation','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','Annotation','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Annotation Layers','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_query','Api','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_query_form_data','Api','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_time_range','Api','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list','AsyncEventsRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','AvailableDomains','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_invalidate','CacheRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_export','Chart','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Chart','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','Chart','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Charts','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_get','ColumnarToDatabaseView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_post','ColumnarToDatabaseView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','CSS Templates','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','CssTemplate','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','CssTemplate','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_get','CsvToDatabaseView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_post','CsvToDatabaseView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete_embedded','Dashboard','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_export','Dashboard','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get_embedded','Dashboard','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Dashboard','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','Dashboard','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','DashboardFilterStateRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','DashboardFilterStateRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','DashboardPermalinkRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','DashboardPermalinkRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Dashboards','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Data','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Database','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Databases','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_duplicate','Dataset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_export','Dataset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get_or_create_dataset','Dataset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Dataset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','Dataset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Datasets','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_external_metadata','Datasource','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_external_metadata_by_name','Datasource','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','Datasource','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get_column_values','Datasource','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_samples','Datasource','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_save','Datasource','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list','DynamicPlugin','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_show','DynamicPlugin','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','EmbeddedDashboard','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_get','ExcelToDatabaseView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_post','ExcelToDatabaseView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Explore','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','ExploreFormDataRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','ExploreFormDataRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','ExplorePermalinkRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','ExplorePermalinkRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_add','FilterSets','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','FilterSets','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_edit','FilterSets','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list','FilterSets','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Home','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Import Dashboards','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_export','ImportExportRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_import','ImportExportRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get_value','KV','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_store','KV','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_recent_activity','Log','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Manage','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','MenuApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','OpenApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','Permission','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_info','Permission','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','PermissionViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','PermissionViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_info','PermissionViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_post','PermissionViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_put','PermissionViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Plugins','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Profile','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','ReportSchedule','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','ReportSchedule','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_get','ResetMyPasswordView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_this_form_post','ResetMyPasswordView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_add_role_permissions','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_info','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list_role_permissions','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_post','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_put','Role','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','RowLevelSecurity','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list','SavedQuery','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','SecurityRestApi','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_estimate_query_cost','SQLLab','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_format_sql','SQLLab','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_add_slices','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_annotation_json','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_approve','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_available_domains','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_copy_dash','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_created_dashboards','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_created_slices','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_csv','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_dashboard','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_dashboard_permalink','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_datasources','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_estimate_query_cost','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_explore','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_explore_json','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_extra_table_metadata','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_fave_dashboards','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_fave_dashboards_by_username','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_fave_slices','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_favstar','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_fetch_datasource_metadata','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_filter','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_import_dashboards','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_log','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_override_role_permissions','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_profile','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_queries','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_recent_activity','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_request_access','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_results','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_save_dash','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_schemas_access_for_file_upload','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_search_queries','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_share_chart','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_share_dashboard','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_slice','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_slice_json','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_sql_json','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_sqllab_table_viz','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_sqllab_viz','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_stop_query','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_tables','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_testconn','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_user_slices','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_validate_sql_json','Superset','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_show','SwaggerView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','TableSchemaView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_expanded','TableSchemaView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_post','TableSchemaView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_bulk_create','Tag','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Tag','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','Tag','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_add','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_download','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_edit','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_list','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_show','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Tags','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','TagView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','TagView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_post','TagView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_suggestions','TagView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_tagged_objects','TagView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_tags','TagView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','User','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','User','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_info','User','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_post','User','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_put','User','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_userinfo','UserOAuthModelView','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_delete','ViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_get','ViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_info','ViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_post','ViewMenu','BI_EDITOR_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_put','ViewMenu','BI_EDITOR_TEMP');


--
-- Add default permissions for BI_VIEWER_TEMP role in temp_permission_view_role
--
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_dashboard','Superset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_explore_json','Superset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_fave_dashboards','Superset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_csv','Superset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_favstar','Superset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_profile','Superset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Chart','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','CssTemplate','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Dashboard','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Database','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','Dataset','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_recent_activity','Log','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_userinfo','UserDBModelView','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('menu_access','Dashboards','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('all_datasource_access','all_datasource_access','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_read','DashboardFilterStateRestApi','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_write','DashboardFilterStateRestApi','BI_VIEWER_TEMP');
INSERT INTO temp_permission_view_role(permission_name, view_name, role_name) VALUES ('can_log','Superset','BI_VIEWER_TEMP');



--
-- Delete BI_ADMIN_TEMP, BI_EDITOR_TEMP, BI_VIEWER_TEMP permissions (might happen when script gets stuck)
--
DELETE FROM
    ab_permission_view_role
WHERE
    role_id IN (SELECT ab_role.id FROM ab_role WHERE ab_role.name IN ('BI_ADMIN_TEMP', 'BI_EDITOR_TEMP', 'BI_VIEWER_TEMP'));


--
-- Delete BI_ADMIN_TEMP, BI_EDITOR_TEMP, BI_VIEWER_TEMP roles (might happen when script gets stuck)
--
DELETE FROM
    ab_role
WHERE
    name IN ('BI_ADMIN_TEMP', 'BI_EDITOR_TEMP', 'BI_VIEWER_TEMP');


--
-- Create BI_ADMIN_TEMP, BI_EDITOR_TEMP and BI_VIEWER_TEMP
--
INSERT INTO ab_role VALUES (nextval('ab_role_id_seq'), 'BI_ADMIN_TEMP');
INSERT INTO ab_role VALUES (nextval('ab_role_id_seq'), 'BI_EDITOR_TEMP');
INSERT INTO ab_role VALUES (nextval('ab_role_id_seq'), 'BI_VIEWER_TEMP');


--
-- Create the initial permissions according to temp_permission_view_role table
--
INSERT INTO
    ab_permission_view_role(id, permission_view_id, role_id)
SELECT
    nextval('ab_permission_view_role_id_seq'),
    (
        SELECT
            ab_permission_view.id
        FROM
            ab_permission_view
                JOIN ab_permission ON ab_permission.id = permission_id
                JOIN ab_view_menu ON ab_view_menu.id = view_menu_id
        WHERE (ab_permission.name = temp_permission_view_role.permission_name AND ab_view_menu.name = temp_permission_view_role.view_name)
    ) ab_permission_view_id,
    (
        SELECT
            ab_role.id
        FROM
            ab_role
        WHERE
                name = temp_permission_view_role.role_name
    ) ab_role_id

FROM
    temp_permission_view_role;


--
-- Update all users that have BI_ADMIN to the temporary BI_ADMIN_TEMP role
--
UPDATE ab_user_role SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_ADMIN_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_ADMIN');


--
-- Update all users that have BI_EDITOR to the temporary BI_EDITOR_TEMP role
--
UPDATE ab_user_role SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_EDITOR_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_EDITOR');


--
-- Update all users that have BI_VIEWER to the temporary BI_VIEWER_TEMP role
--
UPDATE ab_user_role SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_VIEWER_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_VIEWER');


--
-- Delete all permissions associated with BI_ADMIN, BI_EDITOR or BI_VIEWER
--
DELETE FROM
    ab_permission_view_role
WHERE
    role_id IN (SELECT ab_role.id FROM ab_role WHERE ab_role.name IN ('BI_ADMIN', 'BI_EDITOR', 'BI_VIEWER'));


--
-- Update dashboards that have BI_ADMIN in RBAC to the temporary BI_ADMIN_TEMP role
--
UPDATE dashboard_roles SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_ADMIN_TEMP')
WHERE
        role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_ADMIN');

--
-- Update dashboards that have BI_EDITOR in RBAC to the temporary BI_EDITOR_TEMP role
--
UPDATE dashboard_roles SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_EDITOR_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_EDITOR');


--
-- Update dashboards that have BI_VIEWER in RBAC to the temporary BI_VIEWER_TEMP role
--
UPDATE dashboard_roles SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_VIEWER_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_VIEWER');


--
-- Update row level security filters that have BI_ADMIN role to the temporary BI_ADMIN_TEMP role
--
UPDATE rls_filter_roles SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_ADMIN_TEMP')
WHERE
        role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_ADMIN');


--
-- Update row level security filters that have BI_EDITOR role to the temporary BI_EDITOR_TEMP role
--
UPDATE rls_filter_roles SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_EDITOR_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_EDITOR');


--
-- Update row level security filters that have BI_VIEWER role to the temporary BI_VIEWER_TEMP role
--
UPDATE rls_filter_roles SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_VIEWER_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'BI_VIEWER');


--
-- Delete the old BI_ADMIN, BI_EDITOR, BI_VIEWER roles
--
DELETE FROM ab_role WHERE name IN ('BI_ADMIN', 'BI_EDITOR', 'BI_VIEWER');


--
-- Update name of temporary BI_ADMIN_TEMP role to BI_ADMIN
--
UPDATE ab_role SET name = 'BI_ADMIN' WHERE name = 'BI_ADMIN_TEMP';


--
-- Update name of temporary BI_EDITOR_TEMP role to BI_EDITOR
--
UPDATE ab_role SET name = 'BI_EDITOR' WHERE name = 'BI_EDITOR_TEMP';


--
-- Update name of temporary BI_VIEWER_TEMP role to BI_VIEWER
--
UPDATE ab_role SET name = 'BI_VIEWER' WHERE name = 'BI_VIEWER_TEMP';