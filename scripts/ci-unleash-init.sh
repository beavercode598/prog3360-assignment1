#!/usr/bin/env bash
set -euo pipefail

UNLEASH_URL="${UNLEASH_URL:-http://localhost:4242}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-unleash4all}"
PROJECT="${PROJECT:-default}"
ENVIRONMENT="${ENVIRONMENT:-development}"

COOKIE_JAR="$(mktemp)"

echo "==> Wait for Unleash health"
for i in {1..60}; do
  if curl -fsS "${UNLEASH_URL}/health" >/dev/null; then
    break
  fi
  sleep 2
done

echo "==> Login (get session cookie)"
curl -sS -c "$COOKIE_JAR" -X POST "${UNLEASH_URL}/auth/simple/login" \
  -H "Content-Type: application/json" \
  --data-binary "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" >/dev/null

echo "==> Create backend token for services (development env)"
TOKEN_JSON="$(curl -sS -b "$COOKIE_JAR" -X POST "${UNLEASH_URL}/api/admin/projects/${PROJECT}/api-tokens" \
  -H "Content-Type: application/json" \
  --data-binary "{\"type\":\"backend\",\"tokenName\":\"prog3360-services\",\"environment\":\"${ENVIRONMENT}\"}")"

UNLEASH_API_TOKEN="$(echo "$TOKEN_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['secret'])")"
echo "UNLEASH_API_TOKEN=${UNLEASH_API_TOKEN}" >> "$GITHUB_ENV"

echo "==> Create features (idempotent)"
create_feature () {
  local name="$1"
  # 201 created, 409 exists
  code="$(curl -sS -o /dev/null -w "%{http_code}" -b "$COOKIE_JAR" -X POST \
    "${UNLEASH_URL}/api/admin/projects/${PROJECT}/features" \
    -H "Content-Type: application/json" \
    --data-binary "{\"name\":\"${name}\"}")"
  if [[ "$code" != "201" && "$code" != "409" ]]; then
    echo "Failed creating feature ${name}, HTTP ${code}"
    exit 1
  fi
}

create_feature "premium-pricing"
create_feature "bulk-order-discount"
create_feature "order-notifications"

echo "==> Turn ON all flags in ${ENVIRONMENT}"
toggle_on () {
  local name="$1"
  curl -sS -o /dev/null -b "$COOKIE_JAR" -X POST \
    "${UNLEASH_URL}/api/admin/projects/${PROJECT}/features/${name}/environments/${ENVIRONMENT}/on"
}

toggle_on "premium-pricing"
toggle_on "bulk-order-discount"
toggle_on "order-notifications"

echo "==> Unleash init done"