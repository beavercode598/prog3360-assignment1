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
    echo "Services are healthy"
    break
  fi
  echo "Waiting for services... ($i/60)"
  sleep 2
done

echo "==> Create product (price=100)"
PRODUCT_JSON="$(curl -sS -X POST "${PRODUCT_URL}/api/products" \
  -H "Content-Type: application/json" \
  --data-binary '{"name":"Test","price":100.0,"quantity":10}')"

PRODUCT_ID="$(echo "$PRODUCT_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")"
echo "Product id=${PRODUCT_ID}"

# Helper: login once for admin endpoints (cookie-based)
login_unleash() {
  curl -sS -c "$COOKIE_JAR" -X POST "${UNLEASH_URL}/auth/simple/login" \
    -H "Content-Type: application/json" \
    --data-binary "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" >/dev/null
}

# Helper: toggle a flag on/off
toggle_flag() {
  local flag="$1"
  local state="$2" # "on" or "off"
  curl -sS -o /dev/null -b "$COOKIE_JAR" -X POST \
    "${UNLEASH_URL}/api/admin/projects/${PROJECT}/features/${flag}/environments/${ENVIRONMENT}/${state}"
}

# Helper: assert premium endpoint eventually reflects expected price
wait_for_premium_price() {
  local expected="$1"
  local last=""

  for i in {1..30}; do
    last="$(curl -sS "${PRODUCT_URL}/api/products/premium")"
    price="$(echo "$last" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['price'])")"
    echo "Attempt $i: premium price=$price (expect $expected)"
    if [ "$price" = "$expected" ]; then
      echo "$last" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d[0]['price']==${expected}, d"
      return 0
    fi
    sleep 2
  done

  echo "ERROR: premium price did not become ${expected} in time. Last response:"
  echo "$last"
  return 1
}

# Helper: create an order and return JSON
create_order() {
  local qty="$1"
  curl -sS -X POST "${ORDER_URL}/api/orders" \
    -H "Content-Type: application/json" \
    --data-binary "{\"productId\":${PRODUCT_ID},\"quantity\":${qty}}"
}

# Helper: assert order total eventually reflects expected total
wait_for_order_total() {
  local qty="$1"
  local expected="$2"
  local last=""

  for i in {1..30}; do
    last="$(create_order "$qty")"
    total="$(echo "$last" | python3 -c "import sys,json; o=json.load(sys.stdin); print(o['totalPrice'])")"
    echo "Attempt $i: qty=$qty totalPrice=$total (expect $expected)"
    if [ "$total" = "$expected" ]; then
      echo "$last" | python3 -c "import sys,json; o=json.load(sys.stdin); assert o['totalPrice']==${expected}, o"
      return 0
    fi
    sleep 2
  done

  echo "ERROR: totalPrice did not become ${expected} in time. Last response:"
  echo "$last"
  return 1
}

# --- PREMIUM PRICING ---
echo "==> premium-pricing ON => premium price 90.0"
# (Assumes your init script already created and enabled premium-pricing; we just wait for propagation)
wait_for_premium_price "90.0"

# --- BULK ORDER DISCOUNT ON ---
echo "==> bulk-order-discount ON & qty=6 => totalPrice 510.0"
# 100 * 6 = 600; 15% off => 510.0
wait_for_order_total "6" "510.0"

# --- TOGGLE OFF BULK ORDER DISCOUNT ---
echo "==> Turn OFF bulk-order-discount and verify qty=6 => totalPrice 600.0"
login_unleash
toggle_flag "bulk-order-discount" "off"

# Wait until the OFF state propagates to order-service (prevents CI flake)
wait_for_order_total "6" "600.0"

echo "==> Integration tests OK"