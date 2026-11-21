#!/usr/bin/env bash
set -euo pipefail

sdk="${ANDROID_HOME:?Use a runner with the Android SDK installed}"
if [[ ! -f "$sdk/platforms/android-36/android.jar" || ! -x "$sdk/build-tools/35.0.0/aapt" ]]; then
    "$sdk/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$sdk" "platforms;android-36" "build-tools;35.0.0"
fi
test -f "$sdk/platforms/android-36/android.jar"
test -x "$sdk/build-tools/35.0.0/aapt"
