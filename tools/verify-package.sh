#!/usr/bin/env bash
set -euo pipefail

apk="${1:?Pass the release APK}"
aab="${2:?Pass the release AAB}"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:?Set ANDROID_HOME to the Android SDK path}}"
build_tools="$sdk/build-tools/35.0.0"
gradle_cache="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

if [[ "${VERIFY_SIGNATURES:-true}" == true ]]; then
    "$build_tools/apksigner" verify --verbose --print-certs "$apk" > "$tmp/apk-signature"
    jarsigner -verify "$aab" > "$tmp/aab-signature"
    grep -Fq 'jar verified.' "$tmp/aab-signature"
    apk_cert="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$tmp/apk-signature")"
    aab_cert="$(keytool -printcert -jarfile "$aab" | sed -n 's/^[[:space:]]*SHA256: //p' | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')"
    [[ -n "$apk_cert" && "$apk_cert" == "$aab_cert" ]]
fi
"$build_tools/zipalign" -c -P 16 4 "$apk"
"$build_tools/aapt" dump badging "$apk" > "$tmp/badging"
"$build_tools/aapt" dump xmltree "$apk" AndroidManifest.xml > "$tmp/apk-manifest"
bundletool="$(find "$gradle_cache/com.android.tools.build/bundletool/1.18.1" "$gradle_cache/com.android.tools.build/bundletool" -name 'bundletool-*.jar' -print -quit 2>/dev/null || true)"
test -n "$bundletool"
dependencies="$(find "$gradle_cache" -name '*.jar' -printf '%p:' | sed 's/:$//')"
bundle=(java -cp "$bundletool:$dependencies" com.android.tools.build.bundletool.BundleToolMain)
"${bundle[@]}" validate --bundle="$aab"
"${bundle[@]}" dump manifest --bundle="$aab" > "$tmp/aab-manifest"
"${bundle[@]}" dump config --bundle="$aab" > "$tmp/aab-config"
grep -Fq 'PAGE_ALIGNMENT_16K' "$tmp/aab-config"

python3 - "$tmp" "$apk" "$aab" <<'PY'
import pathlib
import re
import sys
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile

root, apk, aab = (pathlib.Path(value) for value in sys.argv[1:])
badging = (root / "badging").read_text()
apk_manifest = (root / "apk-manifest").read_text()
assert "package: name='com.fruitcatcher.game' versionCode='1' versionName='1.0.0'" in badging
assert "sdkVersion:'26'" in badging
assert "targetSdkVersion:'36'" in badging
assert "application-label:'Fruit Catcher'" in badging
assert re.search(r"uses-feature-not-required: name='android.hardware.screen.portrait'", badging)
assert re.search(r"uses-feature: name='android.hardware.touchscreen'", badging)
assert not re.search(r"E: (uses-permission|permission)", apk_manifest)

ns = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "aab-manifest").getroot()
assert manifest.attrib["package"] == "com.fruitcatcher.game"
assert manifest.attrib[ns + "versionCode"] == "1" and manifest.attrib[ns + "versionName"] == "1.0.0"
assert manifest.find("uses-sdk").attrib[ns + "targetSdkVersion"] == "36"
assert manifest.find("uses-sdk").attrib[ns + "minSdkVersion"] == "26"
features = {element.attrib[ns + "name"]: element.get(ns + "required", "true")
            for element in manifest.findall("uses-feature")}
assert features == {"android.hardware.screen.portrait": "false", "android.hardware.touchscreen": "true"}, features
assert not any(node.tag.startswith("uses-permission") or node.tag == "permission" for node in manifest)
app = manifest.find("application")
assert app.attrib[ns + "icon"]
assert app.attrib[ns + "allowBackup"] == "false"
assert app.find("activity").attrib[ns + "screenOrientation"] == "portrait"

for artifact in (apk, aab):
    with zipfile.ZipFile(artifact) as archive:
        assert archive.testzip() is None
        if artifact == aab:
            assert any("icon_foreground" in path for path in archive.namelist()), artifact
        libraries = [entry for entry in archive.namelist() if entry.endswith(".so")]
        with tempfile.TemporaryDirectory() as folder:
            for entry in libraries:
                path = archive.extract(entry, folder)
                headers = subprocess.check_output(["readelf", "-Wl", path], text=True)
                segments = [line.split() for line in headers.splitlines() if line.lstrip().startswith("LOAD")]
                assert segments and all(int(segment[-1], 16) >= 16384 for segment in segments), entry
        print(f"{artifact.suffix}: checked {len(libraries)} native library files for 16 KB ELF alignment.")
print("APK and AAB package, SDKs, icon resources, portrait, permissions, touchscreen, and 16 KB packaging verified.")
PY
if [[ -f "$tmp/apk-signature" ]]; then cat "$tmp/apk-signature"; fi
