#!/usr/bin/env bash
set -Eeuo pipefail
umask 027

APP_ROOT="/opt/campus-business"
INCOMING_ROOT="$APP_ROOT/incoming"
RELEASES_ROOT="$APP_ROOT/releases"
SCRIPTS_ROOT="$APP_ROOT/scripts"
CURRENT_FILE="$APP_ROOT/CURRENT_RELEASE"
BACKEND_TARGET="$APP_ROOT/campus-business-backend.jar"
WEB_ROOT="/www/wwwroot/hutbxyw.click"
SERVICE="campus-business"
LOCAL_HEALTH="http://127.0.0.1:18080/api/actuator/health"
PUBLIC_HOME="https://hutbxyw.click/"
PUBLIC_HEALTH="https://hutbxyw.click/api/actuator/health"
LOCK_FILE="$APP_ROOT/deploy.lock"

PREVIOUS_RELEASE=""
CHANGES_STARTED=0

log() {
  printf '[deploy] %s\n' "$*"
}

fail() {
  log "ERROR: $*"
  return 1
}

require_root() {
  [[ "${EUID:-$(id -u)}" -eq 0 ]] || fail "must run as root through sudo"
}

validate_sha() {
  [[ "$1" =~ ^[0-9a-f]{40}$ ]] || fail "release id must be a full 40-character lowercase Git SHA"
}

wait_for_new_health() {
  local response=""
  local attempt
  for attempt in $(seq 1 30); do
    if response="$(curl --silent --show-error --fail --max-time 4 "$LOCAL_HEALTH" 2>/dev/null)" \
      && grep -q '"status":"UP"' <<< "$response"; then
      return 0
    fi
    sleep 1
  done
  return 1
}

verify_public_endpoints() {
  local homepage health
  homepage="$(curl --silent --show-error --fail --location --max-time 10 "$PUBLIC_HOME")"
  [[ -n "$homepage" ]] || fail "public homepage is empty"
  grep -q '<div id="app"></div>' <<< "$homepage" || fail "public homepage does not contain the application mount point"
  health="$(curl --silent --show-error --fail --max-time 10 "$PUBLIC_HEALTH")"
  grep -q '"status":"UP"' <<< "$health" || fail "public health endpoint is not UP"
}

deploy_frontend() {
  local source_dir="$1"
  local item base temporary_index
  [[ -s "$source_dir/index.html" ]] || fail "frontend index.html is missing"
  [[ -d "$source_dir/assets" ]] || fail "frontend assets directory is missing"

  install -d -m 0755 "$WEB_ROOT" "$WEB_ROOT/assets"
  cp -a "$source_dir/assets/." "$WEB_ROOT/assets/"

  shopt -s dotglob nullglob
  for item in "$source_dir"/*; do
    base="$(basename "$item")"
    [[ "$base" == "index.html" || "$base" == "assets" ]] && continue
    cp -a "$item" "$WEB_ROOT/"
  done
  shopt -u dotglob nullglob

  temporary_index="$WEB_ROOT/.index.html.$$.next"
  install -m 0644 "$source_dir/index.html" "$temporary_index"
  mv -f "$temporary_index" "$WEB_ROOT/index.html"
  chown -R www:www "$WEB_ROOT"
}

snapshot_legacy_release() {
  local legacy_id legacy_dir
  legacy_id="legacy-$(date -u +'%Y%m%dT%H%M%SZ')"
  legacy_dir="$RELEASES_ROOT/$legacy_id"
  log "creating one-time snapshot of the pre-CI production version: $legacy_id"
  install -d -m 0750 "$legacy_dir/frontend"
  [[ -s "$BACKEND_TARGET" ]] || fail "current backend JAR is missing; cannot create initial rollback snapshot"
  [[ -s "$WEB_ROOT/index.html" ]] || fail "current frontend is missing; cannot create initial rollback snapshot"
  cp -a "$BACKEND_TARGET" "$legacy_dir/backend.jar"
  cp -a "$WEB_ROOT/." "$legacy_dir/frontend/"
  printf 'COMMIT_SHA=%s\nBUILT_AT=%s\n' "$legacy_id" "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" > "$legacy_dir/release.env"
  PREVIOUS_RELEASE="$legacy_id"
}

read_previous_release() {
  if [[ -s "$CURRENT_FILE" ]]; then
    PREVIOUS_RELEASE="$(tr -d '[:space:]' < "$CURRENT_FILE")"
    [[ -n "$PREVIOUS_RELEASE" && -d "$RELEASES_ROOT/$PREVIOUS_RELEASE" ]] \
      || fail "CURRENT_RELEASE points to a missing release"
  else
    snapshot_legacy_release
  fi
}

validate_incoming_release() {
  local incoming_dir="$1"
  local jar_size archive_size
  [[ -s "$incoming_dir/backend.jar" ]] || fail "incoming backend.jar is missing"
  [[ -s "$incoming_dir/admin-dist.tar.gz" ]] || fail "incoming admin-dist.tar.gz is missing"
  [[ -s "$incoming_dir/release.env" ]] || fail "incoming release.env is missing"
  [[ -s "$incoming_dir/checksums.sha256" ]] || fail "incoming checksums.sha256 is missing"

  [[ "$(awk '{print $2}' "$incoming_dir/checksums.sha256" | sort | tr '\n' ' ')" == "admin-dist.tar.gz backend.jar release.env " ]] \
    || fail "checksums.sha256 contains an unexpected file list"
  (cd "$incoming_dir" && sha256sum --strict -c checksums.sha256)
  grep -qx "COMMIT_SHA=$RELEASE_ID" "$incoming_dir/release.env" \
    || fail "release.env does not match requested commit"

  jar_size="$(stat -c '%s' "$incoming_dir/backend.jar")"
  archive_size="$(stat -c '%s' "$incoming_dir/admin-dist.tar.gz")"
  (( jar_size >= 10485760 && jar_size <= 209715200 )) || fail "backend JAR size is outside the allowed range"
  (( archive_size >= 102400 && archive_size <= 104857600 )) || fail "frontend archive size is outside the allowed range"
  unzip -tqq "$incoming_dir/backend.jar" >/dev/null

  if tar -tzf "$incoming_dir/admin-dist.tar.gz" \
    | awk '$0 ~ /^\// || $0 ~ /(^|\/)\.\.(\/|$)/ { found=1 } END { exit(found ? 0 : 1) }'; then
    fail "frontend archive contains an unsafe path"
  fi
  if tar -tvzf "$incoming_dir/admin-dist.tar.gz" | awk 'substr($1,1,1) !~ /[-d]/ { found=1 } END { exit(found ? 0 : 1) }'; then
    fail "frontend archive contains a link or special file"
  fi
}

prepare_release_directory() {
  local incoming_dir="$1"
  local release_dir="$RELEASES_ROOT/$RELEASE_ID"
  local staging_dir="$RELEASES_ROOT/.staging-$RELEASE_ID-$$"

  if [[ -d "$release_dir" ]]; then
    [[ "$RELEASE_ID" != "$PREVIOUS_RELEASE" ]] || fail "requested commit is the current release; use rollback for older releases"
    rm -rf -- "$release_dir"
  fi
  rm -rf -- "$staging_dir"
  install -d -m 0750 "$staging_dir/frontend"
  cp -a "$incoming_dir/backend.jar" "$staging_dir/backend.jar"
  cp -a "$incoming_dir/release.env" "$staging_dir/release.env"
  cp -a "$incoming_dir/checksums.sha256" "$staging_dir/checksums.sha256"
  tar --extract --gzip --file "$incoming_dir/admin-dist.tar.gz" \
    --directory "$staging_dir/frontend" --no-same-owner --no-same-permissions

  [[ -s "$staging_dir/frontend/index.html" ]] || fail "extracted frontend index.html is missing"
  [[ -d "$staging_dir/frontend/assets" ]] || fail "extracted frontend assets directory is missing"
  [[ -n "$(find "$staging_dir/frontend/assets" -type f -name '*.js' -print -quit)" ]] || fail "frontend JavaScript asset is missing"
  [[ -n "$(find "$staging_dir/frontend/assets" -type f -name '*.css' -print -quit)" ]] || fail "frontend CSS asset is missing"
  printf '%s\n' "$PREVIOUS_RELEASE" > "$staging_dir/previous-release"
  mv "$staging_dir" "$release_dir"
}

activate_release() {
  local release_dir="$RELEASES_ROOT/$RELEASE_ID"
  local backend_next="$APP_ROOT/.campus-business-backend.jar.$$.next"
  CHANGES_STARTED=1

  deploy_frontend "$release_dir/frontend"
  install -m 0640 "$release_dir/backend.jar" "$backend_next"
  chown root:root "$backend_next"
  mv -f "$backend_next" "$BACKEND_TARGET"
  systemctl restart "$SERVICE"
  systemctl is-active --quiet "$SERVICE"
  wait_for_new_health || fail "local application health check failed"
  verify_public_endpoints

  printf '%s\n' "$RELEASE_ID" > "$CURRENT_FILE.next"
  chmod 0644 "$CURRENT_FILE.next"
  mv -f "$CURRENT_FILE.next" "$CURRENT_FILE"
}

cleanup_old_releases() {
  local current previous name count=0
  current="$(tr -d '[:space:]' < "$CURRENT_FILE")"
  previous="$(tr -d '[:space:]' < "$RELEASES_ROOT/$current/previous-release" 2>/dev/null || true)"
  while IFS= read -r name; do
    [[ "$name" =~ ^[0-9a-f]{40}$ ]] || continue
    count=$((count + 1))
    if (( count > 5 )) && [[ "$name" != "$current" && "$name" != "$previous" ]]; then
      log "removing old ordinary release $name"
      rm -rf -- "$RELEASES_ROOT/$name"
    fi
  done < <(find "$RELEASES_ROOT" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %f\n' \
    | sort -nr | awk '{print $2}')
}

on_error() {
  local exit_code=$?
  local line="${1:-unknown}"
  trap - ERR
  log "deployment failed at line $line"
  if (( CHANGES_STARTED == 1 )) && [[ -n "$PREVIOUS_RELEASE" && -d "$RELEASES_ROOT/$PREVIOUS_RELEASE" ]]; then
    log "attempting automatic rollback to $PREVIOUS_RELEASE"
    if CAMPUS_DEPLOY_LOCK_HELD=1 "$SCRIPTS_ROOT/rollback.sh" "$PREVIOUS_RELEASE" --automatic; then
      log "automatic rollback succeeded"
    else
      log "CRITICAL: automatic rollback failed; inspect $SERVICE immediately"
    fi
  fi
  exit "$exit_code"
}

main() {
  require_root
  [[ $# -eq 1 ]] || fail "usage: deploy.sh <40-character-commit-sha>"
  RELEASE_ID="$1"
  validate_sha "$RELEASE_ID"

  install -d -m 0750 "$RELEASES_ROOT" "$SCRIPTS_ROOT"
  exec 9>"$LOCK_FILE"
  flock -n 9 || fail "another deployment or rollback is already running"
  trap 'on_error $LINENO' ERR

  local incoming_dir="$INCOMING_ROOT/$RELEASE_ID"
  validate_incoming_release "$incoming_dir"
  read_previous_release
  prepare_release_directory "$incoming_dir"
  activate_release
  rm -rf -- "$incoming_dir"
  cleanup_old_releases
  log "deployment succeeded: $RELEASE_ID"
}

main "$@"
