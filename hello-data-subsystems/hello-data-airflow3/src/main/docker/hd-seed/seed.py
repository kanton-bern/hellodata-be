import os, sys, time
from sqlalchemy import create_engine, text

engine = create_engine(os.environ["AIRFLOW__DATABASE__SQL_ALCHEMY_CONN"], pool_pre_ping=True)
# SQL ships next to this script inside the image (baked in the same hd-seed dir).
sql = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "05_hd_technical_user.sql")).read()

# The permission grants are name-based and need the api-server's FAB bootstrap (Viewer role +
# can_read on the Users/Roles/Permissions view-menus), which happens at api-server BOOT. Poll
# until that exists before seeding, so the grants actually land.
deadline = time.time() + 840
while True:
    try:
        with engine.connect() as c:
            viewer = c.execute(text("select count(*) from ab_role where name='Viewer'")).scalar()
            vm = c.execute(text(
                "select count(distinct vm.name) from ab_permission_view pv "
                "join ab_permission p on p.id=pv.permission_id and p.name='can_read' "
                "join ab_view_menu vm on vm.id=pv.view_menu_id "
                "where vm.name in ('Users','Roles','Permissions')")).scalar()
        if viewer and vm == 3:
            print("FAB bootstrap present (Viewer + can_read Users/Roles/Permissions)", flush=True)
            break
        print("waiting for api-server FAB bootstrap: viewer=%s view_menus=%s" % (viewer, vm), flush=True)
    except Exception as e:
        print("waiting for DB/api-server: %s" % e, flush=True)
    if time.time() > deadline:
        print("timed out waiting for FAB bootstrap", file=sys.stderr)
        sys.exit(1)
    time.sleep(10)

raw = engine.raw_connection()
try:
    cur = raw.cursor(); cur.execute(sql); raw.commit()
finally:
    raw.close()

with engine.connect() as c:
    roles = [r[0] for r in c.execute(text(
        "select r.name from ab_role r join ab_user_role ur on ur.role_id=r.id where ur.user_id=900000"))]
    perms = c.execute(text(
        "select count(*) from ab_permission_view_role pvr "
        "join ab_permission_view pv on pv.id=pvr.permission_view_id "
        "join ab_view_menu vm on vm.id=pv.view_menu_id and vm.name in ('Users','Roles','Permissions') "
        "join ab_permission p on p.id=pv.permission_id and p.name='can_read' "
        "join ab_role r on r.id=pvr.role_id and r.name='hd_sidecar_monitor'")).scalar()
    hd_base_perms = c.execute(text(
        "select count(*) from ab_permission_view_role pvr "
        "join ab_role r on r.id=pvr.role_id and r.name='hd_base'")).scalar()
print("seed applied: user 900000 roles=%s monitor_read_perms=%s hd_base_perms=%s"
      % (roles, perms, hd_base_perms), flush=True)
if "hd_sidecar_monitor" not in roles or (perms or 0) < 3 or (hd_base_perms or 0) < 20:
    print("seed verification FAILED", file=sys.stderr); sys.exit(1)
print("seed OK", flush=True)
