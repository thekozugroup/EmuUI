#!/usr/bin/env sh
# Copies the composed screenshot sets into the fastlane supply layout just before an upload.
# They stay out of Git there: qa/check_publication.py rejects any indexed phoneScreenshots path.
set -eu
root=$(cd "$(dirname "$0")/../.." && pwd)
images="$root/fastlane/metadata/android/en-US/images"
for pair in phone:phoneScreenshots sevenInch:sevenInchScreenshots tenInch:tenInchScreenshots; do
  from="$root/store/play/${pair%%:*}"
  to="$images/${pair#*:}"
  rm -rf "$to"
  mkdir -p "$to"
  cp "$from"/*.png "$to"/
done
cp "$root/store/play/icon.png" "$root/store/play/featureGraphic.png" "$root/store/play/tvBanner.png" "$images"/
echo "Staged Play images in $images"
