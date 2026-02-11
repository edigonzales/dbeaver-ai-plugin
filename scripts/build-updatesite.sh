#!/usr/bin/env bash
set -euo pipefail

ECLIPSE_BIN="${ECLIPSE_BIN:-/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse}"
STAGING_DIR="${1:-/Users/stefan/sources/dbeaver-release-staging/update-input}"
OUTPUT_DIR="${2:-/tmp/dbeaver-updatesite}"
CATEGORY_XML="${3:-/Users/stefan/sources/dbeaver-ilitools-feature/category.xml}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

check_exists() {
  local path="$1"
  [[ -e "$path" ]] || fail "Missing required path: $path"
}

check_glob() {
  local pattern="$1"
  compgen -G "$pattern" >/dev/null || fail "Missing required artifact: $pattern"
}

check_exists "$ECLIPSE_BIN"
check_exists "$STAGING_DIR"
check_exists "$CATEGORY_XML"

check_glob "$STAGING_DIR/features/ch.so.agi.dbeaver.ili2pg.feature_*.jar"
check_glob "$STAGING_DIR/plugins/ch.so.agi.dbeaver.ili2pg_*.jar"
check_glob "$STAGING_DIR/features/ch.so.agi.dbeaver.ai.feature_*.jar"
check_glob "$STAGING_DIR/plugins/ch.so.agi.dbeaver.ai_*.jar"

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

"$ECLIPSE_BIN" \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:${OUTPUT_DIR}/" \
  -artifactRepository "file:${OUTPUT_DIR}/" \
  -source "$STAGING_DIR" \
  -compress \
  -publishArtifacts

"$ECLIPSE_BIN" \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
  -metadataRepository "file:${OUTPUT_DIR}/" \
  -categoryDefinition "file:${CATEGORY_XML}" \
  -categoryQualifier "interlis"

check_exists "$OUTPUT_DIR/content.jar"
check_exists "$OUTPUT_DIR/artifacts.jar"

content_tmp="$(mktemp)"
artifacts_tmp="$(mktemp)"
trap 'rm -f "$content_tmp" "$artifacts_tmp"' EXIT

unzip -p "$OUTPUT_DIR/content.jar" >"$content_tmp"
unzip -p "$OUTPUT_DIR/artifacts.jar" >"$artifacts_tmp"

grep -q "ch.so.agi.dbeaver.ili2pg" "$content_tmp" || fail "content.jar does not reference ili2pg"
grep -q "ch.so.agi.dbeaver.ai" "$content_tmp" || fail "content.jar does not reference ai plugin"
grep -q "ch.so.agi.dbeaver.ili2pg.feature.feature.group" "$content_tmp" || fail "content.jar missing ili2pg feature group"
grep -q "ch.so.agi.dbeaver.ai.feature.feature.group" "$content_tmp" || fail "content.jar missing ai feature group"

grep -q "id='ch.so.agi.dbeaver.ili2pg.feature'" "$artifacts_tmp" || fail "artifacts.jar missing ili2pg feature artifact"
grep -q "id='ch.so.agi.dbeaver.ai.feature'" "$artifacts_tmp" || fail "artifacts.jar missing ai feature artifact"
grep -q "id='ch.so.agi.dbeaver.ili2pg'" "$artifacts_tmp" || fail "artifacts.jar missing ili2pg plugin artifact"
grep -q "id='ch.so.agi.dbeaver.ai'" "$artifacts_tmp" || fail "artifacts.jar missing ai plugin artifact"

echo "Update site successfully generated: $OUTPUT_DIR"
echo "Next step: upload content of $OUTPUT_DIR to https://dbeaver.sogeo.services/updates/"
