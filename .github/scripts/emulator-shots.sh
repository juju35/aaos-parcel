#!/usr/bin/env bash
# Runs the APK on the booted emulator: install, launch, type the "demo" API key, then screenshots of
# the list, a delivery and the settings, and one run of the hourly check. Fails on a crash of the app.
# The templates host draws the app in a surface: no view tree to read, taps go by position
# (fractions of the screen, from the template layout).
# Usage: emulator-shots.sh <apk> <out dir> <name prefix>
set -uo pipefail
APK=$1 OUT=$2 PREFIX=$3
PKG=me.jstephan.parcel
mkdir -p "$OUT"

# Main display (the emulator also has the instrument cluster).
DISPLAY_ID=$(adb shell dumpsys SurfaceFlinger --display-id 2>/dev/null | sed -n 's/^Display \([0-9]*\).*/\1/p' | head -1)
read -r W H < <(adb shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
echo "::notice::display ${DISPLAY_ID:-default} ${W}x${H}"

shot() {
  sleep "${2:-3}"
  adb exec-out screencap ${DISPLAY_ID:+-d $DISPLAY_ID} -p > "$OUT/$PREFIX-$1.png"
  echo "::notice::screenshot $PREFIX-$1 ($(wc -c < "$OUT/$PREFIX-$1.png") bytes)"
}

# Tap at fractions of the screen: tap 0.25 0.3
tap() {
  adb shell input tap "$(awk -v f="$1" -v w="$W" 'BEGIN{printf "%d", f*w}')" "$(awk -v f="$2" -v h="$H" 'BEGIN{printf "%d", f*h}')"
}

keyboard_shown() {
  adb shell dumpsys input_method | grep -q "mInputShown=true"
}

crashed() {
  adb logcat -d -b crash | grep -q "Process: $PKG"
}

# Errors the templates host shows instead of the app ("has encountered an unexpected error").
host_error() {
  adb logcat -d | grep -q -E "CarApp\.H[^:]*: Error:"
}

report_host_error() {
  adb logcat -d | grep -E "CarApp\.H[^:]*: (Error|	at |Caused)" | head -20 | while read -r l; do echo "::error::$l"; done
  exit 1
}

report_crash() {
  # The exception and the app frames, not the 100 lines of binder plumbing.
  adb logcat -d -b crash | grep -E "FATAL|Process:|Exception|Error|Caused by|me\.jstephan" | head -25 \
    | while read -r l; do echo "::error::$l"; done
  exit 1
}

check() {
  crashed && report_crash
  host_error && report_host_error
}

# -g grants the notification permission (Android 13+), as accepting it in the car would.
adb install -r -g "$APK" | tail -1
adb logcat -c

# No key yet: the setup message with its "Configurer" button.
adb shell am start -W -n "$PKG/androidx.car.app.activity.CarAppActivity" | grep -E "Status|TotalTime" | while read -r l; do echo "::notice::$l"; done
shot setup 6
check

# The button sits under the centred message; its height depends on the screen, so try down the
# middle until the API key screen opens its keyboard.
for y in 0.58 0.62 0.54 0.66 0.70 0.74 0.78 0.82; do
  tap 0.5 "$y"
  sleep 3
  if keyboard_shown; then
    echo "::notice::API key screen opened by a tap at y=$y"
    break
  fi
done
if ! keyboard_shown; then
  # Maybe the key screen without its keyboard: tap where its field is.
  tap 0.5 0.45
  sleep 2
  keyboard_shown || echo "::warning::no keyboard: the API key screen may not have opened"
fi
shot key 1
adb shell input text "demo"
adb shell input keyevent 66
check

# The demo deliveries; carrier names come from Parcel's public list (network).
shot list 10
check

# Positions in the template, landscape and portrait differ.
# First row under the header; back arrow of the header at (56, 126); the settings icon is the
# last action of the strip, at the top right.
if [ "$W" -gt "$H" ]; then
  ROW="0.40 0.32"; BACK="0.055 0.165"; SETTINGS_X=$(awk -v w="$W" 'BEGIN{printf "%.3f", (w-72)/w}'); SETTINGS_Y=0.165
else
  ROW="0.40 0.20"; BACK="0.07 0.10"; SETTINGS_X=$(awk -v w="$W" 'BEGIN{printf "%.3f", (w-72)/w}'); SETTINGS_Y=0.10
fi

tap $ROW
shot detail 5
check
tap $BACK
sleep 3
tap "$SETTINGS_X" "$SETTINGS_Y"
shot settings 5
check

# One run of the hourly check (WorkManager on JobScheduler): it must not crash.
JOB=$(adb shell dumpsys jobscheduler 2>/dev/null | grep -o "JOB #u[0-9]*a[0-9]*/[0-9]*: [0-9a-f]* $PKG/androidx.work" | head -1 | sed 's#.*/\([0-9]*\):.*#\1#')
if [ -n "$JOB" ]; then
  adb shell cmd jobscheduler run -f "$PKG" "$JOB" | while read -r l; do echo "::notice::check job $JOB: $l"; done
  sleep 10
  check
else
  echo "::warning::hourly check job not found"
fi

adb logcat -d | grep -E " (E|W) (DeliveryList|DeliveryDetail|DeliveryRepository|CheckWorker|AndroidRuntime|CarApp)" | tail -20 \
  | while read -r l; do echo "::warning::$l"; done
exit 0
