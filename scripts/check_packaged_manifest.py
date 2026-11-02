"""Assert security invariants in AGP's packaged manifest, after the selected build."""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("variant", choices=("debug", "release"))
variant = parser.parse_args().variant
android = "{http://schemas.android.com/apk/res/android}"
manifest = Path(f"app/build/intermediates/packaged_manifests/{variant}/process{variant.title()}ManifestForPackage/AndroidManifest.xml")
app = ET.parse(manifest).getroot().find("application")
assert app is not None
assert app.get(android + "allowBackup") == "false", "Backup must remain disabled"
assert app.get(android + "usesCleartextTraffic") == "false", "Cleartext traffic must remain disabled"
if variant == "release":
    assert app.get(android + "debuggable", "false") == "false", "Release must not be debuggable"
activities = app.findall("activity")
main = next(a for a in activities if a.get(android + "name") == "com.waypoint.app.MainActivity")
assert main.get(android + "exported") == "true", "Destination activity must remain available"
preview = [a for a in activities if a.get(android + "name") == "com.waypoint.app.WorldPreviewCaptureActivity"]
if variant == "debug":
    assert len(preview) == 1 and preview[0].get(android + "exported") == "false", "Debug preview must stay private"
else:
    assert not preview, "Debug preview must not be packaged in the release"
services = app.findall("service")
assert all(s.get(android + "exported") == "false" for s in services), "A service became exported"
car = next(s for s in services if s.get(android + "name") == "com.waypoint.app.WaypointCarService")
assert car.get(android + "enabled") == "false", "Experimental Android Auto must remain disabled"
assert not any(m.get(android + "name") == "com.google.android.gms.car.application" for m in app.findall("meta-data")), "Disabled integration must not be advertised"
providers = app.findall("provider")
assert all(p.get(android + "exported") == "false" for p in providers), "A provider became exported"
updater = next(p for p in providers if p.get(android + "name") == "com.waypoint.app.UpdateFileProvider")
assert updater.get(android + "authorities") == "com.waypoint.app.updates"
assert updater.get(android + "grantUriPermissions") == "true"
print(f"Packaged {variant} manifest security invariants verified")
