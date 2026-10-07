#!/bin/sh
set -eu

package_name="com.nexa.finance"
activity="com.nexa.finance/.MainActivity"
samples=""

echo "Measuring cold startup with adb am start -S -W (5 samples)..."

i=1
while [ "$i" -le 5 ]; do
  adb shell am force-stop "$package_name" >/dev/null
  output="$(adb shell am start -S -W -n "$activity" 2>&1)"
  total_time="$(printf '%s\n' "$output" | awk -F': ' '$1 == "TotalTime" {print $2; exit}')"
  case "$total_time" in
    ''|*[!0-9]*)
      printf "Invalid TotalTime on sample %s\n" "$i"
      printf "%s\n" "$output"
      exit 1
      ;;
  esac
  printf "startup_sample_%s_ms=%s\n" "$i" "$total_time"
  if [ -n "$samples" ]; then
    samples="$samples $total_time"
  else
    samples="$total_time"
  fi
  i=$((i + 1))
done

median="$(printf '%s\n' $samples | sort -n | awk '{ values[NR] = $1 } END { if (NR % 2 == 1) print values[(NR + 1) / 2]; else print values[NR / 2] }')"
average="$(printf '%s\n' $samples | awk '{ sum += $1 } END { if (NR > 0) printf "%.1f", sum / NR }')"
printf "NEXA_STARTUP_COLD_MS median_ms=%s average_ms=%s samples=%s\n" "$median" "$average" "$samples"
