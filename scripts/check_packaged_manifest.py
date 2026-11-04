"""Assert security invariants in AGP's packaged manifest, after the selected build."""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("variant", choices=("debug", "release", "preview"))
variant = parser.parse_args().variant
package = "com.waypoint.app.preview" if variant == "preview" else "com.waypoint.app"
android = "{http://schemas.android.com/apk/res/android}"
manifest = Path(f"app/build/intermediates/packaged_manifests/{variant}/process{variant.title()}ManifestForPackage/AndroidManifest.xml")
root = ET.parse(manifest).getroot()
assert root.get("package") == package, "Unexpected application identity"
app = root.find("application")
assert app is not None
assert app.get(android + "allowBackup") == "false", "Backup must remain disabled"
assert app.get(android + "usesCleartextTraffic") == "false", "Cleartext traffic must remain disabled"
if variant == "release":
    assert app.get(android + "debuggable", "false") == "false", "Release must not be debuggable"
activities = app.findall("activity")
main = next(a for a in activities if a.get(android + "name") == "com.waypoint.app.MainActivity")
assert main.get(android + "exported") == "true", "Destination activity must remain available"
assert not any(a.get(android + "name") == "androidx.compose.ui.tooling.PreviewActivity" for a in activities), "External Compose previews must not be packaged"
assert all(a is main or a.get(android + "exported") == "false" for a in activities), "Unexpected activity became exported"
def has_intent(action, category, scheme=None):
    return any(
        any(a.get(android + "name") == action for a in f.findall("action"))
        and any(c.get(android + "name") == category for c in f.findall("category"))
        and (scheme is None or any(d.get(android + "scheme") == scheme for d in f.findall("data")))
        for f in main.findall("intent-filter")
    )
assert has_intent("androidx.car.app.action.NAVIGATE", "android.intent.category.DEFAULT", "geo"), "Android Auto navigation handoff missing"
assert has_intent("android.intent.action.NAVIGATE", "android.intent.category.DEFAULT", "geo"), "Phone navigation handoff missing"
assert has_intent("android.intent.action.MAIN", "android.intent.category.APP_MAPS"), "Maps selector entry missing"
assert has_intent("android.intent.action.SEND", "android.intent.category.DEFAULT"), "Shared text entry missing"
assert has_intent("android.intent.action.PROCESS_TEXT", "android.intent.category.DEFAULT"), "Selected text entry missing"
receivers = app.findall("receiver")
quest_receiver = next(r for r in receivers if r.get(android + "name") == "com.waypoint.app.QuestAlarmReceiver")
assert quest_receiver.get(android + "exported") == "false", "Quest actions must stay app-private"
rules = ET.parse("app/src/main/res/xml/data_extraction_rules.xml").getroot()
for policy in ("cloud-backup", "device-transfer"):
    exclusions = {(item.get("domain"), item.get("path")) for item in rules.find(policy).findall("exclude")}
    assert {(domain, ".") for domain in ("sharedpref", "database", "file")} <= exclusions, "Private quest data must remain excluded from backup and transfer"
preview = [a for a in activities if a.get(android + "name") == "com.waypoint.app.WorldPreviewCaptureActivity"]
if variant in ("debug", "preview"):
    assert len(preview) == 1 and preview[0].get(android + "exported") == "false", "Debug preview must stay private"
else:
    assert not preview, "Debug preview must not be packaged in the release"
services = app.findall("service")
car = next(s for s in services if s.get(android + "name") == "com.waypoint.app.WaypointCarService")
assert car.get(android + "enabled", "true") == "true", "Android Auto service must be enabled"
assert car.get(android + "exported") == "true", "Android Auto host must be able to bind"
update_job = next(s for s in services if s.get(android + "name") == "com.waypoint.app.UpdateCheckJob")
assert update_job.get(android + "exported") == "true" and update_job.get(android + "permission") == "android.permission.BIND_JOB_SERVICE", "Only Android may bind the update job"
assert all(s is car or s is update_job or s.get(android + "exported") == "false" for s in services), "Unexpected service became exported"
car_filters = car.findall("intent-filter")
assert any(
    any(a.get(android + "name") == "androidx.car.app.CarAppService" for a in f.findall("action"))
    and any(c.get(android + "name") == "androidx.car.app.category.NAVIGATION" for c in f.findall("category"))
    for f in car_filters
), "Navigation car-service discovery filter missing"
assert any(m.get(android + "name") == "com.google.android.gms.car.application" and m.get(android + "resource") == "@xml/automotive_app_desc" for m in app.findall("meta-data")), "Android Auto descriptor missing"
assert any(m.get(android + "name") == "androidx.car.app.minCarApiLevel" and m.get(android + "value") == "2" for m in app.findall("meta-data")), "Minimum car API metadata missing"
permissions = {p.get(android + "name") for p in root.findall("uses-permission")}
assert "android.permission.ACCESS_MOCK_LOCATION" not in permissions, "Mock-location capability must not be packaged"
assert {"androidx.car.app.MAP_TEMPLATES", "androidx.car.app.NAVIGATION_TEMPLATES", "androidx.car.app.ACCESS_SURFACE"} <= permissions, "Car map/navigation permissions missing"
descriptor = ET.parse("app/src/main/res/xml/automotive_app_desc.xml").getroot()
assert descriptor.tag == "automotiveApp" and any(u.get("name") == "template" for u in descriptor.findall("uses")), "Car template capability missing"
providers = app.findall("provider")
assert all(p.get(android + "exported") == "false" for p in providers), "A provider became exported"
updater = next(p for p in providers if p.get(android + "name") == "com.waypoint.app.UpdateFileProvider")
assert updater.get(android + "authorities") == package + ".updates"
assert updater.get(android + "grantUriPermissions") == "true"
print(f"Packaged {variant} manifest security invariants verified")
