#!/bin/bash
# AroundU Stress Test Runner - Multi-User Simulation
# Runs Vegeta tests locally against a target URL

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
TARGET_URL="${TARGET_URL:-http://localhost:8080}"
RESULTS_DIR="$(dirname "$0")/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Check dependencies
check_deps() {
    if ! command -v vegeta &> /dev/null; then
        print_error "Vegeta not found. Install with: brew install vegeta"
        exit 1
    fi
    print_status "Vegeta version: $(vegeta --version | head -1)"
}

# Check target health
check_target() {
    print_status "Checking target: $TARGET_URL"
    if curl -sf "$TARGET_URL/actuator/health" > /dev/null 2>&1; then
        print_status "Target is healthy"
    else
        print_error "Target not reachable at $TARGET_URL"
        exit 1
    fi
}

# Run single stress test
run_test() {
    local NAME="$1"
    local RPS="$2"
    local DURATION="$3"
    local TARGET="$4"
    local WORKERS="${5:-50}"
    
    echo -e "\n${YELLOW}>>> Testing: $NAME at ${RPS} RPS for ${DURATION}${NC}"
    
    RESULT_FILE="$RESULTS_DIR/${NAME}_${RPS}rps_${TIMESTAMP}"
    
    echo "$TARGET" | vegeta attack \
        -rate="$RPS" \
        -duration="$DURATION" \
        -workers="$WORKERS" \
        -connections="$WORKERS" \
        -timeout=10s \
    | tee "${RESULT_FILE}.bin" \
    | vegeta report -type=text
    
    # Save JSON report
    vegeta report -type=json < "${RESULT_FILE}.bin" > "${RESULT_FILE}.json"
    
    echo ""
}

# Multi-user simulation test
run_multiuser_test() {
    local RPS="$1"
    local DURATION="${2:-30s}"
    
    print_header "Multi-User Simulation @ $RPS RPS"
    
    # Simulate different user actions
    echo "Simulating multiple users accessing different endpoints..."
    
    # Create mixed targets file for realistic traffic
    cat > /tmp/mixed_targets.txt << EOF
GET $TARGET_URL/actuator/health

GET $TARGET_URL/api/v1/fx/rate?from=USD&to=INR

GET $TARGET_URL/api/v1/jobs

POST $TARGET_URL/api/v1/auth/login
Content-Type: application/json

{"email":"user1@test.com","password":"test123"}

POST $TARGET_URL/api/v1/auth/login
Content-Type: application/json

{"email":"user2@test.com","password":"test123"}

GET $TARGET_URL/actuator/prometheus

EOF

    RESULT_FILE="$RESULTS_DIR/multiuser_${RPS}rps_${TIMESTAMP}"
    
    vegeta attack \
        -targets=/tmp/mixed_targets.txt \
        -rate="$RPS" \
        -duration="$DURATION" \
        -workers=100 \
        -connections=100 \
        -timeout=10s \
    | tee "${RESULT_FILE}.bin" \
    | vegeta report -type=text
    
    vegeta report -type=json < "${RESULT_FILE}.bin" > "${RESULT_FILE}.json"
    
    # Show status code distribution
    echo -e "\n${YELLOW}Status Code Distribution:${NC}"
    vegeta report -type=hist[0,5ms,10ms,50ms,100ms,500ms,1s,2s,5s] < "${RESULT_FILE}.bin"
}

# Main
main() {
    print_header "AroundU Stress Test Suite"
    
    check_deps
    mkdir -p "$RESULTS_DIR"
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --url)
                TARGET_URL="$2"
                shift 2
                ;;
            --quick)
                QUICK_MODE=true
                shift
                ;;
            *)
                shift
                ;;
        esac
    done
    
    check_target
    
    print_header "Test Plan"
    echo "Target: $TARGET_URL"
    echo "Results: $RESULTS_DIR"
    echo ""
    
    if [[ "$QUICK_MODE" == "true" ]]; then
        # Quick test - single RPS level
        run_test "health" 100 "10s" "GET $TARGET_URL/actuator/health"
        run_multiuser_test 100 "15s"
    else
        # Full test suite
        print_header "Phase 1: Health Endpoint Scaling Test"
        for RPS in 100 500 1000 2000; do
            run_test "health" "$RPS" "20s" "GET $TARGET_URL/actuator/health"
            echo "Cooling down for 5s..."
            sleep 5
        done
        
        print_header "Phase 2: Multi-User Simulation"
        for RPS in 100 500 1000; do
            run_multiuser_test "$RPS" "20s"
            echo "Cooling down for 10s..."
            sleep 10
        done
        
        print_header "Phase 3: Rate Limit Testing"
        echo "Testing auth endpoint to trigger rate limits..."
        run_test "auth_ratelimit" 200 "30s" "POST $TARGET_URL/api/v1/auth/login
Content-Type: application/json

{\"email\":\"ratelimit@test.com\",\"password\":\"test123\"}" 25
    fi
    
    print_header "Test Complete!"
    echo "Results saved to: $RESULTS_DIR"
    echo ""
    echo "Generate report with:"
    echo "  vegeta report < $RESULTS_DIR/multiuser_*_${TIMESTAMP}.bin"
}

main "$@"
