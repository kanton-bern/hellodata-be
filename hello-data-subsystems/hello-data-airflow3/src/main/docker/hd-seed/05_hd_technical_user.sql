-- Technical service user for the HelloDATA Airflow-3 sidecar's read-only REST polling.
-- FAB user ids are DB-assigned; seed a service user with a FIXED high id (900000) that never
-- collides with real users (which start at 1). The sidecar pins AIRFLOW_API_USER_ID=900000.
INSERT INTO ab_user (id, first_name, last_name, username, email, active, password, login_count, fail_login_count, created_on, changed_on)
SELECT 900000, 'HelloDATA', 'Sidecar', 'hd-sidecar', 'hd-sidecar@hellodata.local', true, NULL, 0, 0, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM ab_user WHERE id = 900000 OR username = 'hd-sidecar'
);

INSERT INTO ab_user_role (id, user_id, role_id)
SELECT 900000, 900000, r.id
FROM ab_role r
WHERE r.name = 'Viewer'
  AND NOT EXISTS (
      SELECT 1 FROM ab_user_role WHERE user_id = 900000 AND role_id = r.id
  );

-- Read role for the FAB auth-manager API (GET /auth/fab/v1/{users,roles,permissions}), which
-- Viewer lacks. Dedicated role so other viewers are unaffected.
INSERT INTO ab_role (id, name)
SELECT 900001, 'hd_sidecar_monitor'
WHERE NOT EXISTS (SELECT 1 FROM ab_role WHERE name = 'hd_sidecar_monitor');

INSERT INTO ab_permission_view_role (id, permission_view_id, role_id)
SELECT 900000 + row_number() OVER (ORDER BY pv.id), pv.id, r.id
FROM ab_permission_view pv
JOIN ab_permission p ON p.id = pv.permission_id AND p.name = 'can_read'
JOIN ab_view_menu vm ON vm.id = pv.view_menu_id AND vm.name IN ('Users', 'Roles', 'Permissions')
CROSS JOIN (SELECT id FROM ab_role WHERE name = 'hd_sidecar_monitor') r
WHERE NOT EXISTS (
    SELECT 1 FROM ab_permission_view_role x WHERE x.permission_view_id = pv.id AND x.role_id = r.id
);

INSERT INTO ab_user_role (id, user_id, role_id)
SELECT 900001, 900000, r.id
FROM ab_role r
WHERE r.name = 'hd_sidecar_monitor'
  AND NOT EXISTS (
      SELECT 1 FROM ab_user_role WHERE user_id = 900000 AND role_id = r.id
  );

-- ---------------------------------------------------------------------------------------------
-- hd_base: the "UI shell" FAB role that `airflow_base` maps to (AUTH_ROLES_MAPPING in the image's
-- webserver_config.py). FAB only ASSIGNS mapped roles at login, it never CREATES them, and the
-- image ships no bootstrap for hd_base -> so it is missing and every DATA_DOMAIN_ADMIN user
-- (mapped to airflow_base) lands with 0 permissions -> 403 on the DAG list. Seed it here (same
-- FAB-bootstrap precondition as above). Grant it every Viewer read/menu permission EXCEPT the
-- collective `can_read on DAGs` (all-DAGs read): per-domain DAG visibility must come from the
-- DD_<domain> role (per-DAG access_control stamped by the dag_policy), so the base role must not
-- be able to read all DAGs. Fixed ids (900002 / 900100+) to stay idempotent and collision-free.
INSERT INTO ab_role (id, name)
SELECT 900002, 'hd_base'
WHERE NOT EXISTS (SELECT 1 FROM ab_role WHERE name = 'hd_base');

INSERT INTO ab_permission_view_role (id, permission_view_id, role_id)
SELECT 900100 + row_number() OVER (ORDER BY pv.id), pv.id, hb.id
FROM ab_permission_view pv
JOIN ab_permission_view_role vpvr ON vpvr.permission_view_id = pv.id
JOIN ab_role viewer ON viewer.id = vpvr.role_id AND viewer.name = 'Viewer'
CROSS JOIN (SELECT id FROM ab_role WHERE name = 'hd_base') hb
WHERE NOT (
    pv.permission_id = (SELECT id FROM ab_permission WHERE name = 'can_read')
    AND pv.view_menu_id = (SELECT id FROM ab_view_menu WHERE name = 'DAGs')
)
AND NOT EXISTS (
    SELECT 1 FROM ab_permission_view_role x
    WHERE x.permission_view_id = pv.id AND x.role_id = hb.id
);
