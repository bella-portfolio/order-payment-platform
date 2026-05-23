#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# E2E Test Script for Order/Payment Platform
# Tests both success and failure scenarios end-to-end.
# =============================================================================

ORDER_API="http://localhost:8081/api/orders"
PASS=0
FAIL=0

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# --- Helpers ---
check_status() {
    local order_id="$1"
    local expected_status="$2"
    local description="$3"

    # Poll for up to 30 seconds (the outbox poller runs every 2 seconds)
    for i in $(seq 1 15); do
        local status
        status=$(curl -s "${ORDER_API}/${order_id}" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$status" = "$expected_status" ]; then
            echo -e "  ${GREEN}✓${NC} ${description}: status=${status}"
            PASS=$((PASS + 1))
            return 0
        fi
        echo "  Waiting for status=${expected_status} (current: ${status})... attempt ${i}/15"
        sleep 2
    done

    echo -e "  ${RED}✗${NC} ${description}: expected=${expected_status}, got=${status:-timeout}"
    FAIL=$((FAIL + 1))
    return 1
}

# --- Check prerequisites ---
echo -e "${YELLOW}=== Checking Prerequisites ===${NC}"
if ! curl -s --connect-timeout 2 "${ORDER_API}" > /dev/null 2>&1; then
    echo -e "${RED}Order Service not reachable at ${ORDER_API}${NC}"
    echo "Make sure docker-compose and both services are running."
    exit 1
fi
echo -e "${GREEN}✓${NC} Order Service is reachable"

# =============================================================================
# Test 1: Happy path — create order, expect PAID
# =============================================================================
echo ""
echo -e "${YELLOW}=== Test 1: Happy Path (PAID) ===${NC}"

ORDER1=$(curl -s -X POST "${ORDER_API}" \
    -H "Content-Type: application/json" \
    -d '{
        "productId": "PROD-001",
        "quantity": 2,
        "amount": 15000.00
    }')

ORDER1_ID=$(echo "$ORDER1" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "  Created order: id=${ORDER1_ID}"
echo "  Response: ${ORDER1}"

check_status "$ORDER1_ID" "PAID" "Order should be PAID"

# =============================================================================
# Test 2: Failure path — create order with FAIL-001 product, expect CANCELLED
# =============================================================================
echo ""
echo -e "${YELLOW}=== Test 2: Failure Path (CANCELLED) ===${NC}"

ORDER2=$(curl -s -X POST "${ORDER_API}" \
    -H "Content-Type: application/json" \
    -d '{
        "productId": "FAIL-001",
        "quantity": 1,
        "amount": 99999.00
    }')

ORDER2_ID=$(echo "$ORDER2" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "  Created order: id=${ORDER2_ID}"
echo "  Response: ${ORDER2}"

check_status "$ORDER2_ID" "CANCELLED" "Order should be CANCELLED (Saga compensation)"

# =============================================================================
# Summary
# =============================================================================
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  E2E Test Results${NC}"
echo -e "${YELLOW}========================================${NC}"
echo -e "  Passed: ${GREEN}${PASS}${NC}"
if [ $FAIL -gt 0 ]; then
    echo -e "  Failed: ${RED}${FAIL}${NC}"
else
    echo -e "  Failed: ${FAIL}"
fi
echo ""

if [ $FAIL -gt 0 ]; then
    exit 1
fi

echo -e "${GREEN}All tests passed!${NC}"
exit 0
