#!/bin/bash
# AroundU Java Backend - Stress Test Runner
# Uses Vegeta for load testing (same as Go backend)
# Supports both local (Docker) and in-cluster (K8s) testing

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
TARGETS_DIR="${SCRIPT_DIR}/targets"
K8S_DIR="$(dirname "$SCRIPT_DIR")/k8s"
NAMESPACE="aroundu"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Default configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
DURATION="${DURATION:-30s}"
RPS_LEVELS="${RPS_LEVELS:-1000,5000,10000,20000}"
MODE="${MODE:-local}"  # local or k8s
AUTH_TOKEN="${AUTH_TOKEN:-}"

print_banner() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════════════╗"
    echo "║        AroundU Java Backend - Stress Test Suite                   ║"
    echo "║        Powered by Vegeta Load Testing                             ║"
    echo "╚═══════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

check_vegeta() {
    if ! command -v vegeta &> /dev/null; then
        echo -e "${RED}✗ Vegeta not found. Installing...${NC}"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew install vegeta
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            go install github.com/tsenart/vegeta@latest
        else
            echo -e "${RED}Please install Vegeta manually: https://github.com/tsenart/vegeta${NC}"
            exit 1
        fi
    fi
    echo -e "${GREEN}✓ Vegeta available: $(vegeta --version 2>&1 | head -1)${NC}"
}

check_target_health() {
    echo -e "${YELLOW}Checking target health: ${BASE_URL}/actuator/health${NC}"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health" --max-time 5 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ Target is healthy (HTTP 200)${NC}"
        return 0
    else
        echo -e "${RED}✗ Target unhealthy or unreachable (HTTP ${HTTP_CODE})${NC}"
        return 1
    fi
}

run_warmup() {
    echo -e "${CYAN}🔥 Running warmup phase (100 RPS for 10s)...${NC}"
    echo "GET ${BASE_URL}/actuator/health" | vegeta attack -rate=100 -duration=10s | vegeta report -type=text
    echo -e "${GREEN}✓ Warmup complete${NC}"
    sleep 3
}

run_load_test() {
    local rps=$1
    local name=$2
    local target_file=$3
    local timestamp=$(date +%Y-%m-%d_%H-%M-%S)
    
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  Testing: ${name} @ ${rps} RPS for ${DURATION}${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════════${NC}"
    
    local workers=$((rps / 10 + 1))
    [ $workers -gt $rps ] && workers=$rps
    
    # Run attack and save binary results
    local bin_file="${RESULTS_DIR}/${name}_${rps}rps_${timestamp}.bin"
    
    cat "$target_file" | vegeta attack \
        -rate=$rps \
        -duration=$DURATION \
        -timeout=30s \
        -workers=$workers \
        -max-workers=$rps \
        | tee "$bin_file" | vegeta report -type=text
    
    # Generate JSON report
    vegeta report -type=json < "$bin_file" > "${RESULTS_DIR}/${name}_${rps}rps_${timestamp}.json" 2>/dev/null || true
    
    # Generate histogram
    vegeta report -type=hist[0,5ms,10ms,25ms,50ms,100ms,250ms,500ms,1s,5s] < "$bin_file" 2>/dev/null || true
    
    echo ""
}

generate_targets() {
    echo -e "${YELLOW}Generating target files...${NC}"
    mkdir -p "$TARGETS_DIR"
    
    # Health check target (unauthenticated)
    cat > "${TARGETS_DIR}/health.txt" <<EOF
GET ${BASE_URL}/actuator/health
Accept: application/json
EOF

    # Prometheus metrics target (unauthenticated after our fix)
    cat > "${TARGETS_DIR}/metrics.txt" <<EOF
GET ${BASE_URL}/actuator/prometheus
Accept: text/plain
EOF

    # Jobs listing (requires auth)
    if [ -n "$AUTH_TOKEN" ]; then
        cat > "${TARGETS_DIR}/jobs-list.txt" <<EOF
GET ${BASE_URL}/api/v1/jobs?page=0&size=10
Accept: application/json
Authorization: Bearer ${AUTH_TOKEN}
EOF

        # User profile (requires auth)
        cat > "${TARGETS_DIR}/user-profile.txt" <<EOF
GET ${BASE_URL}/api/v1/users/me
Accept: application/json
Authorization: Bearer ${AUTH_TOKEN}
EOF
    fi
    
    echo -e "${GREEN}✓ Target files generated in ${TARGETS_DIR}${NC}"
}

generate_report() {
    local timestamp=$(date +%Y-%m-%d_%H-%M-%S)
    local md_file="${RESULTS_DIR}/stress_test_report_${timestamp}.md"
    
    echo -e "${CYAN}Generating summary report...${NC}"
    
    cat > "$md_file" <<EOF
# AroundU Java Backend - Stress Test Report

**Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Target:** ${BASE_URL}
**Duration per test:** ${DURATION}
**RPS Levels:** ${RPS_LEVELS}
**Mode:** ${MODE}

## Test Configuration

| Parameter | Value |
|-----------|-------|
| Base URL | ${BASE_URL} |
| Test Duration | ${DURATION} |
| Connection Reuse | Enabled (default) |
| Timeout | 30s |

## Results Summary

See individual JSON files in \`results/\` for detailed metrics.

### Key Metrics to Watch

- **Throughput**: Actual requests/second achieved
- **Success Rate**: Should be >99% for healthy service
- **P99 Latency**: 99th percentile response time
- **Error Rate**: Any errors indicate capacity limits

## Auto-Scaling Verification

Monitor HPA during tests:
\`\`\`bash
kubectl get hpa -n aroundu -w
kubectl get pods -n aroundu -w
\`\`\`

Expected behavior:
- CPU > 60% → Scale up
- Memory > 80% → Scale up
- Stabilization: 60s for scale-up, 300s for scale-down

## Recommendations

Based on test results:
- 1000 RPS: Baseline single-pod performance
- 5000 RPS: Should trigger auto-scaling (2-3 pods)
- 10000 RPS: Full cluster utilization
- 20000 RPS: Peak load testing (may require >3 pods)

EOF
    
    echo -e "${GREEN}✓ Report saved to ${md_file}${NC}"
}

run_k8s_stress_test() {
    echo -e "${CYAN}Running in-cluster stress test via Kubernetes Job...${NC}"
    
    # Check kubectl access
    if ! kubectl cluster-info &>/dev/null; then
        echo -e "${RED}✗ Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi
    
    # Delete previous job if exists
    kubectl delete job stress-test -n "$NAMESPACE" --ignore-not-found=true 2>/dev/null
    sleep 5
    
    # Apply stress test job
    kubectl apply -f "${K8S_DIR}/stress-test-job.yaml"
    
    # Wait for pod and stream logs
    echo -e "${YELLOW}Waiting for stress test pod to start...${NC}"
    sleep 10
    POD_NAME=$(kubectl get pods -n "$NAMESPACE" -l job-name=stress-test --no-headers 2>/dev/null | awk '{print $1}' | head -1)
    
    if [ -n "$POD_NAME" ]; then
        echo -e "${GREEN}✓ Streaming logs from ${POD_NAME}...${NC}"
        kubectl logs -n "$NAMESPACE" "$POD_NAME" -f 2>/dev/null || true
    fi
    
    # Wait for completion
    kubectl wait --for=condition=complete --timeout=900s job/stress-test -n "$NAMESPACE" 2>/dev/null || {
        echo -e "${RED}✗ Job did not complete within timeout${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ Stress test completed${NC}"
}

main() {
    print_banner
    
    echo "Configuration:"
    echo "  Base URL: ${BASE_URL}"
    echo "  Duration: ${DURATION}"
    echo "  RPS Levels: ${RPS_LEVELS}"
    echo "  Mode: ${MODE}"
    echo ""
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    if [ "$MODE" == "k8s" ]; then
        run_k8s_stress_test
        exit 0
    fi
    
    # Local mode
    check_vegeta
    
    if ! check_target_health; then
        echo -e "${RED}Target is not reachable. Ensure the server is running.${NC}"
        exit 1
    fi
    
    generate_targets
    run_warmup
    
    # Parse RPS levels
    IFS=',' read -ra RPS_ARRAY <<< "$RPS_LEVELS"
    
    # Run tests for each RPS level
    for rps in "${RPS_ARRAY[@]}"; do
        run_load_test "$rps" "health" "${TARGETS_DIR}/health.txt"
        echo -e "${YELLOW}Resting 10 seconds before next test...${NC}"
        sleep 10
    done
    
    generate_report
    
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    All Tests Complete!                            ║${NC}"
    echo -e "${GREEN}╠═══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}║  Results saved to: ${RESULTS_DIR}${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            BASE_URL="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        --rps)
            RPS_LEVELS="$2"
            shift 2
            ;;
        --mode)
            MODE="$2"
            shift 2
            ;;
        --token)
            AUTH_TOKEN="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --url URL        Base URL (default: http://localhost:8080)"
            echo "  --duration DUR   Test duration (default: 30s)"
            echo "  --rps LEVELS     Comma-separated RPS levels (default: 1000,5000,10000,20000)"
            echo "  --mode MODE      Test mode: local or k8s (default: local)"
            echo "  --token TOKEN    Auth token for authenticated endpoints"
            echo ""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

main
