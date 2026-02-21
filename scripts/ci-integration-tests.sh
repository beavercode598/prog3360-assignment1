#!/usr/bin/env bash
set -euo pipefail

PRODUCT_URL="${PRODUCT_URL:-http://localhost:8081}"
ORDER_URL="${ORDER_URL:-http://localhost:8082}"
UNLEASH_URL="${UNLEASH_URL:-http://localhost:4242}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-unleash4all}"
PROJECT="${PROJECT:-default}"
ENVIRONMENT="${ENVIRONMENT:-development}"

COOKIE_JAR="$(mktemp)"

echo "==> Wait for services"
for i in {1..60}; do
  if curl -fsS "${PRODUCT_URL}/actuator/health" >/dev/null \
     && curl -fsS "${ORDER_URL}/actuator/health" >/dev/null; then
    break
  fi
  sleep 2
done

echo "==> Create product (price=100)"
PRODUCT_JSON="$(curl -sS -X POST "${PRODUCT_URL}/api/products" \
  -H "Content-Type: application/json" \
  --data-binary '{"name":"Test","price":100.0,"quantity":10}')"

PRODUCT_ID="$(echo "$PRODUCT_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")"
echo "Product id=${PRODUCT_ID}"

echo "==> premium-pricing ON => premium price 90.0"
PREMIUM_JSON="$(curl -sS "${PRODUCT_URL}/api/products/premium")"
echo "$PREMIUM_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d[0]['price']==90.0, d"

echo "==> bulk-order-discount ON & qty=6 => totalPrice 510.0"
ORDER_JSON="$(curl -sS -X POST "${ORDER_URL}/api/orders" \
  -H "Content-Type: application/json" \
  --data-binary "{\"productId\":${PRODUCT_ID},\"quantity\":6}")"
echo "$ORDER_JSON" | python3 -c "import sys,json; o=json.load(sys.stdin); assert o['totalPrice']==510.0, o"

echo "==> Turn OFF bulk-order-discount and verify qty=6 => 600.0"
curl -sS -c "$COOKIE_JAR" -X POST "${UNLEASH_URL}/auth/simple/login" \
  -H "Content-Type: application/json" \
  --data-binary "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" >/dev/null

curl -sS -o /dev/null -b "$COOKIE_JAR" -X POST \
  "${UNLEASH_URL}/api/admin/projects/${PROJECT}/features/bulk-order-discount/environments/${ENVIRONMENT}/off"

ORDER_JSON2="$(curl -sS -X POST "${ORDER_URL}/api/orders" \
  -H "Content-Type: application/json" \
  --data-binary "{\"productId\":${PRODUCT_ID},\"quantity\":6}")"
echo "$ORDER_JSON2" | python3 -c "import sys,json; o=json.load(sys.stdin); assert o['totalPrice']==600.0, o"

echo "==> Integration tests OK"