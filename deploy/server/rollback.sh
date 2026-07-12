#!/usr/bin/env bash
set -Eeuo pipefail
umask 027

APP_ROOT="/opt/campus-business"
RELEASES_ROOT="$APP_ROOT/releases"
CURRENT_FILE="$APP_ROOT/CURRENT_RELEASE"
BACKEND_TARGET="$APP_ROOT/campus-business-backend.jar"
WEB_ROOT="/www/wwwroot/hutbxyw.click"
SERVICE="campus-business"
LOCAL_HEALTH="http://127.0.0.1:18080/api/actuator/health"
LOCAL_AUTH_CHECK="http://127.0.0.1:18080/api/orders"
PUBLIC_HOME="https://hutbxyw.click/"
PUBLIC_HEALTH="https://hutbxyw.click/api/actuator/health"
PUBLIC_AUTH_CHECK="https://hutbxyw.click/api/orders"
LOCK_FILE="$APP_ROOT/deploy.lock"

log() {
  printf '[rollback] %s\n' "$*"
}

fail() {
  log "ERROR: $*"
  return 1
}

require_root() {
  [[ "${EUID:-$(id -u)}" -eq 0 ]] || fail "must run as root through sudo"
}

validate_release_id() {
  [[ "$1" =~ ^[0-9a-f]{40}$ || "$1" =~ ^legacy-[0-9]{8}T[0-9]{6}Z$ ]] \
    || fail "invalid rollback release id"
}

deploy_frontend() {
  local source_dir="$1"
  local item base temporary_index
  [[ -s "$source_dir/index.html" && -d "$source_dir/assets" ]] || fail "rollback frontend is incomplete"
  install -d -m 0755 "$WEB_ROOT" "$WEB_ROOT/assets"
  cp -a "$source_dir/assets/." "$WEB_ROOT/assets/"
  shopt -s dotglob nullglob
  for item in "$source_dir"/*; do
    base="$(basename "$item")"
    [[ "$base" == "index.html" || "$base" == "assets" ]] && continue
    cp -a "$item" "$WEB_ROOT/"
  done
  shopt -u dotglob nullglob
  temporary_index="$WEB_ROOT/.index.html.$$.rollback"
  install -m 0644 "$source_dir/index.html" "$temporary_index"
  mv -f "$temporary_index" "$WEB_ROOT/index.html"
  chown -R www:www "$WEB_ROOT"
}

wait_for_compatible_health() {
  local attempt response status
  for attempt in $(seq 1 30); do
    if response="$(curl --silent --show-error --fail --max-time 4 "$LOCAL_HEALTH" 2>/dev/null)" \
      && grep -q '"status":"UP"' <<< "$response"; then
      return 0
    fi
    status="$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 4 "$LOCAL_AUTH_CHECK" 2>/dev/null || true)"
    [[ "$status" == "401" ]] && return 0
    sleep 1
  done
  return 1
}

verify_public_endpoints() {
  local homepage response status
  homepage="$(curl --silent --show-error --fail --location --max-time 10 "$PUBLIC_HOME")"
  [[ -n "$homepage" ]] || fail "public homepage is empty after rollback"
  grep -q '<div id="app"></div>' <<< "$homepage" || fail "public homepage is invalid after rollback"
  if response="$(curl --silent --show-error --fail --max-time 10 "$PUBLIC_HEALTH" 2>/dev/null)" \
    && grep -q '"status":"UP"' <<< "$response"; then
    return 0
  fi
  status="$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 "$PUBLIC_AUTH_CHECK" 2>/dev/null || true)"
  [[ "$status" == "401" ]] || fail "public API is unavailable after rollback"
}

resolve_default_target() {
  local current
  [[ -s "$CURRENT_FILE" ]] || fail "CURRENT_RELEASE is missing; specify a release id explicitly"
  current="$(tr -d '[:space:]' < "$CURRENT_FILE")"
  [[ -s "$RELEASES_ROOT/$current/previous-release" ]] \
    || fail "current release has no previous-release pointer"
  tr -d '[:space:]' < "$RELEASES_ROOT/$current/previous-release"
}

main() {
  require_root
  [[ $# -le 2 ]] || fail "usage: rollback.sh [release-id]"
  local target="${1:-}"
  [[ -n "$target" ]] || target="$(resolve_default_target)"
  validate_release_id "$target"

  if [[ "${CAMPUS_DEPLOY_LOCK_HELD:-0}" != "1" ]]; then
    exec 9>"$LOCK_FILE"
    flock -n 9 || fail "another deployment or rollback is already running"
  fi

  local release_dir="$RELEASES_ROOT/$target"
  local backend_next="$APP_ROOT/.campus-business-backend.jar.$$.rollback"
  [[ -s "$release_dir/backend.jar" ]] || fail "rollback backend JAR is missing"
  [[ -s "$release_dir/frontend/index.html" ]] || fail "rollback frontend is missing"
  jar tf "$release_dir/backend.jar" >/dev/null

  log "restoring release $target"
  deploy_frontend "$release_dir/frontend"
  install -m 0640 "$release_dir/backend.jar" "$backend_next"
  chown root:root "$backend_next"
  mv -f "$backend_next" "$BACKEND_TARGET"
  systemctl restart "$SERVICE"
  systemctl is-active --quiet "$SERVICE"
  wait_for_compatible_health || fail "local health check failed after rollback"
  verify_public_endpoints

  printf '%s\n' "$target" > "$CURRENT_FILE.next"
  chmod 0644 "$CURRENT_FILE.next"
  mv -f "$CURRENT_FILE.next" "$CURRENT_FILE"
  log "rollback succeeded: $target"
}

main "$@"
